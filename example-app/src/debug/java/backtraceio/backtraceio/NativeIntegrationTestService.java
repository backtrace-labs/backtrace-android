package backtraceio.backtraceio;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.interfaces.NativeCommunication;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Debug-only base for the native qualification services. Each concrete subclass runs in its own
 * named process (see the debug manifest), so every service observes fresh process-global native
 * state. Commands execute on a {@link HandlerThread}; failures are reported through
 * {@code EVENT_FAILED} carrying only the failure class name and scenario — never exception
 * messages, which can embed credentials or paths.
 */
public abstract class NativeIntegrationTestService extends Service {

    private HandlerThread commandThread;
    private Messenger messenger;

    // Retained while a report may still be uploading; released only on CMD_CLEANUP.
    private BacktraceClient client;
    private BacktraceDatabase database;
    private String guid;
    private String databasePath;
    private String handlerPath;

    @Override
    public void onCreate() {
        super.onCreate();
        commandThread = new HandlerThread(getClass().getSimpleName() + "-commands");
        commandThread.start();
        messenger = new Messenger(new CommandHandler(commandThread));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onDestroy() {
        if (commandThread != null) {
            commandThread.quitSafely();
        }
        super.onDestroy();
    }

    private final class CommandHandler extends Handler {
        CommandHandler(HandlerThread thread) {
            super(thread.getLooper());
        }

        @Override
        public void handleMessage(Message message) {
            final Messenger replyTo = message.replyTo;
            if (replyTo == null) {
                return;
            }
            final int command = message.what;
            final Bundle data = message.getData() == null ? new Bundle() : new Bundle(message.getData());
            try {
                handleCommand(command, data, replyTo);
            } catch (RuntimeException | LinkageError failure) {
                Bundle result = new Bundle();
                if (failure instanceof SafetyScenarioFailure) {
                    result.putString(
                            NativeTestProtocol.KEY_ERROR_TYPE, ((SafetyScenarioFailure) failure).underlyingType);
                    result.putString(NativeTestProtocol.KEY_SCENARIO, ((SafetyScenarioFailure) failure).scenario);
                } else {
                    result.putString(NativeTestProtocol.KEY_ERROR_TYPE, failureType(failure));
                    result.putString(NativeTestProtocol.KEY_SCENARIO, "command-" + command);
                }
                reply(replyTo, NativeTestProtocol.EVENT_FAILED, result);
            }
        }
    }

    private void handleCommand(int command, Bundle data, Messenger replyTo) {
        switch (command) {
            case NativeTestProtocol.CMD_PING:
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, processFacts());
                break;
            case NativeTestProtocol.CMD_RUN_FRESH_PROCESS_SAFETY:
                runFreshProcessSafety();
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, processFacts());
                break;
            case NativeTestProtocol.CMD_PREPARE_REAL_NATIVE:
                prepareRealNative(data.getString(NativeTestProtocol.KEY_GUID), null);
                reply(replyTo, NativeTestProtocol.EVENT_READY, readyFacts());
                break;
            case NativeTestProtocol.CMD_PREPARE_RECOVERY:
                prepareRealNative(
                        data.getString(NativeTestProtocol.KEY_GUID),
                        data.getString(NativeTestProtocol.KEY_DATABASE_PATH));
                reply(replyTo, NativeTestProtocol.EVENT_READY, readyFacts());
                break;
            case NativeTestProtocol.CMD_DUMP:
                requirePrepared();
                client.dumpWithoutCrash(data.getString(NativeTestProtocol.KEY_MESSAGE));
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, null);
                break;
            case NativeTestProtocol.CMD_DISABLE:
                requirePrepared();
                client.disableNativeIntegration();
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, null);
                break;
            case NativeTestProtocol.CMD_ENABLE:
                requirePrepared();
                if (!client.tryEnableNativeIntegration()) {
                    throw new IllegalStateException("Native integration re-enable returned false");
                }
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, null);
                break;
            case NativeTestProtocol.CMD_CRASH:
                requirePrepared();
                reply(replyTo, NativeTestProtocol.EVENT_WILL_CRASH, null);
                client.nativeCrash();
                break;
            case NativeTestProtocol.CMD_CLEANUP:
                if (client != null) {
                    client.disableNativeIntegration();
                }
                client = null;
                database = null;
                reply(replyTo, NativeTestProtocol.EVENT_COMPLETED, null);
                stopSelf();
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    /**
     * Constructs a real client, resolves the handler path <em>after</em> client construction (so
     * {@code dladdr()} observes the loaded library), and requires native integration to enable.
     */
    private void prepareRealNative(String requestedGuid, String existingDatabasePath) {
        requireText(BuildConfig.BACKTRACE_SUBMISSION_URL, "submission URL");

        guid = requestedGuid == null
                ? UUID.randomUUID().toString()
                : UUID.fromString(requestedGuid).toString();

        File databaseRoot = existingDatabasePath == null
                ? new File(getFilesDir(), "native-qualification/" + getClass().getSimpleName() + "-" + guid)
                : new File(existingDatabasePath);

        if (!databaseRoot.exists() && !databaseRoot.mkdirs() && !databaseRoot.isDirectory()) {
            throw new IllegalStateException("Unable to create qualification database");
        }

        Map<String, Object> attributes = new HashMap<>();
        // The native backend reads `guid` during initialization and applies it through Crashpad,
        // so both nonfatal and fatal reports from this process correlate without a custom
        // project-indexed field.
        attributes.put("guid", guid);
        attributes.put("test.native.qualification", getClass().getSimpleName());

        BacktraceCredentials credentials = new BacktraceCredentials(BuildConfig.BACKTRACE_SUBMISSION_URL);
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(databaseRoot.getAbsolutePath());
        database = new BacktraceDatabase(this, settings);
        client = new BacktraceClient(this, credentials, database, attributes, new ArrayList<>());

        handlerPath = findEnvironmentValue(
                new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(getApplicationInfo()),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        if (handlerPath == null || handlerPath.trim().isEmpty()) {
            throw new IllegalStateException("Resolved handler path is blank");
        }

        if (!client.tryEnableNativeIntegration()) {
            throw new IllegalStateException("Native integration returned false");
        }

        databasePath = databaseRoot.getAbsolutePath();
    }

    /**
     * Fresh-process failure safety: every scenario runs before any successful native
     * initialization in this process, then proves both dump overloads and managed persistence
     * survive. Sentinel-bearing failures exercise the sanitized diagnostics on a real device.
     */
    private void runFreshProcessSafety() {
        String sentinels = "https://example.invalid/minidump?token=SECRET_URL_TOKEN_SENTINEL"
                + " --annotation=private.customer.value=PRIVATE_CUSTOMER_SENTINEL"
                + " /data/app/~~opaque/split_config.arm64_v8a.apk!/lib/arm64-v8a/libbacktrace-native.so";

        runSafetyScenario("malformed-credentials", null, new BacktraceCredentials((String) null, "token"));
        runSafetyScenario("bridge-false", stubBridge(null), null);
        runSafetyScenario(
                "bridge-runtime-exception", stubBridge(new IllegalStateException("bridge failed: " + sentinels)), null);
        runSafetyScenario(
                "bridge-linkage-error", stubBridge(new UnsatisfiedLinkError("bridge failed: " + sentinels)), null);
    }

    private void runSafetyScenario(
            String scenario, NativeCommunication bridge, BacktraceCredentials malformedCredentials) {
        try {
            File databaseRoot = new File(getFilesDir(), "fresh-safety/" + scenario + "-" + System.nanoTime());
            if (!databaseRoot.mkdirs() && !databaseRoot.isDirectory()) {
                throw new IllegalStateException("Unable to create safety database");
            }

            // The client is always constructed with valid credentials: a malformed credential is
            // exercised through setupNativeIntegration (the contained path), not the client
            // constructor, which eagerly builds the submission URL.
            BacktraceCredentials validCredentials =
                    new BacktraceCredentials("https://test.sp.backtrace.io", "1231231231231");
            BacktraceCredentials setupCredentials =
                    malformedCredentials != null ? malformedCredentials : validCredentials;
            BacktraceDatabase safetyDatabase =
                    new BacktraceDatabase(this, new BacktraceDatabaseSettings(databaseRoot.getAbsolutePath()));
            BacktraceClient safetyClient = new BacktraceClient(this, validCredentials, safetyDatabase);
            if (bridge != null) {
                safetyDatabase.useNativeCommunication(bridge);
            }
            safetyDatabase.start();

            if (Boolean.TRUE.equals(safetyDatabase.setupNativeIntegration(safetyClient, setupCredentials))) {
                throw new IllegalStateException("Setup unexpectedly succeeded");
            }

            String message = "fresh-process-safety-" + scenario;
            safetyClient.dumpWithoutCrash(message);
            safetyClient.dumpWithoutCrash(message, true);

            BacktraceDatabaseRecord managedRecord =
                    safetyDatabase.add(new BacktraceReport("managed-" + scenario), Collections.emptyMap());
            if (managedRecord == null) {
                throw new IllegalStateException("Managed record was not persisted");
            }
        } catch (RuntimeException | LinkageError failure) {
            throw new SafetyScenarioFailure(scenario, failureType(failure));
        }
    }

    /**
     * Carries the failed scenario name and the underlying failure class (never a message) so the
     * parent's diagnostics identify the scenario without weakening the sanitization invariant.
     */
    private static final class SafetyScenarioFailure extends IllegalStateException {
        final String scenario;
        final String underlyingType;

        SafetyScenarioFailure(String scenario, String underlyingType) {
            super("scenario " + scenario + " failed: " + underlyingType);
            this.scenario = scenario;
            this.underlyingType = underlyingType;
        }
    }

    /** Bridge stub covering every {@link NativeCommunication} method; only Java-handler init is affected. */
    private static NativeCommunication stubBridge(final Object initFailure) {
        return new NativeCommunication() {
            @Override
            public boolean handleCrash(String[] args) {
                return false;
            }

            @Override
            public boolean initializeJavaCrashHandler(
                    String url,
                    String databasePath,
                    String classPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    String[] environmentVariables) {
                if (initFailure instanceof RuntimeException) {
                    throw (RuntimeException) initFailure;
                }
                if (initFailure instanceof LinkageError) {
                    throw (LinkageError) initFailure;
                }
                return false;
            }

            @Override
            public boolean initializeCrashHandler(
                    String url,
                    String databasePath,
                    String handlerPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    boolean enableClientSideUnwinding,
                    UnwindingMode unwindingMode) {
                return false;
            }
        };
    }

    private void requirePrepared() {
        if (client == null) {
            throw new IllegalStateException("Service is not prepared; send CMD_PREPARE_REAL_NATIVE first");
        }
    }

    private Bundle processFacts() {
        Bundle facts = new Bundle();
        facts.putInt(NativeTestProtocol.KEY_PID, Process.myPid());
        facts.putString(NativeTestProtocol.KEY_PROCESS_ABI, AbiHelper.getCurrentAbi());
        facts.putBoolean(
                NativeTestProtocol.KEY_IS_64_BIT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Process.is64Bit());
        return facts;
    }

    private Bundle readyFacts() {
        Bundle facts = processFacts();
        facts.putString(NativeTestProtocol.KEY_GUID, guid);
        facts.putString(NativeTestProtocol.KEY_DATABASE_PATH, databasePath);
        facts.putString(NativeTestProtocol.KEY_HANDLER_PATH, handlerPath);
        return facts;
    }

    private static void reply(Messenger replyTo, int event, Bundle data) {
        Message response = Message.obtain(null, event);
        if (data != null) {
            response.setData(data);
        }
        try {
            replyTo.send(response);
        } catch (RemoteException ignored) {
            // The parent died; nothing meaningful to report to.
        }
    }

    private static String findEnvironmentValue(List<String> environment, String key) {
        String prefix = key + "=";
        for (String value : environment) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return null;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required " + name);
        }
    }

    private static String failureType(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        String type = failure.getClass().getName();
        return type == null || type.trim().isEmpty() ? "unknown" : type;
    }
}
