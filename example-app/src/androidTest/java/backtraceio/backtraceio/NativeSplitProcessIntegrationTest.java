package backtraceio.backtraceio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.coroner.CoronerClient;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Real-handler qualification through dedicated processes: nonfatal ingestion, fatal crash with
 * restart-assisted recovery upload, and disable/re-enable lifecycle. Every gate correlates by GUID
 * (applied through Crashpad at initialization) with a bounded multi-result query, so duplicates
 * are detectable. Skips without backend credentials; the split-path assertion applies on split
 * installs.
 */
@RunWith(AndroidJUnit4.class)
public class NativeSplitProcessIntegrationTest {

    private static final long BIND_TIMEOUT_MS = 20_000;
    private static final long COMMAND_TIMEOUT_MS = 60_000;
    private static final long CRASH_DEATH_TIMEOUT_MS = 20_000;

    private Context context;
    private CoronerClient coroner;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(
                "Backtrace test credentials are not configured",
                hasText(BuildConfig.BACKTRACE_SUBMISSION_URL)
                        && hasText(BuildConfig.BACKTRACE_CORONER_URL)
                        && hasText(BuildConfig.BACKTRACE_CORONER_TOKEN));
        coroner = new CoronerClient(BuildConfig.BACKTRACE_CORONER_URL, BuildConfig.BACKTRACE_CORONER_TOKEN);
    }

    @Test
    public void nonfatalDumpFromDedicatedProcessIsIngestedExactlyOnce() {
        String guid = UUID.randomUUID().toString();
        long timestampStart = nowSeconds() - 5;

        try (RemoteNativeServiceSession session =
                RemoteNativeServiceSession.bind(context, NativeNonfatalTestService.class, BIND_TIMEOUT_MS)) {
            Bundle ready = prepare(session, NativeTestProtocol.CMD_PREPARE_REAL_NATIVE, guid, null);
            verifySplitBackedHandlerPath(ready);

            String message = "split-nonfatal-" + guid;
            Bundle dumpData = new Bundle();
            dumpData.putString(NativeTestProtocol.KEY_MESSAGE, message);
            session.request(
                    NativeTestProtocol.CMD_DUMP, dumpData, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);

            java.util.List<CoronerNativeReportAssertions.NativeReport> reports =
                    CoronerNativeReportAssertions.awaitExactly(
                            coroner, guid, timestampStart, 1, new HashSet<>(Arrays.asList(message)));
            logEvidence(
                    "nonfatal",
                    guid,
                    reports,
                    "\"process_abi\":\"" + ready.getString(NativeTestProtocol.KEY_PROCESS_ABI)
                            + "\",\"process_is_64_bit\":"
                            + ready.getBoolean(NativeTestProtocol.KEY_IS_64_BIT));

            // Cleanup only after ingestion is confirmed; the client must stay alive while
            // uploading.
            session.request(
                    NativeTestProtocol.CMD_CLEANUP, null, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);
        }
    }

    @Test
    public void fatalCrashIsRecoveredAndIngestedExactlyOnce() {
        String guid = UUID.randomUUID().toString();
        long timestampStart = nowSeconds() - 5;
        String databasePath;
        int crashedPid;

        try (RemoteNativeServiceSession fatalSession =
                RemoteNativeServiceSession.bind(context, NativeFatalTestService.class, BIND_TIMEOUT_MS)) {
            Bundle ready = prepare(fatalSession, NativeTestProtocol.CMD_PREPARE_REAL_NATIVE, guid, null);
            verifySplitBackedHandlerPath(ready);
            databasePath = ready.getString(NativeTestProtocol.KEY_DATABASE_PATH);
            crashedPid = fatalSession.getRemotePid();
            assertTrue("Fatal process must report a PID", crashedPid > 0);
            assertNotNull(databasePath);

            fatalSession.send(NativeTestProtocol.CMD_CRASH, null);
            fatalSession.awaitEvent(NativeTestProtocol.EVENT_WILL_CRASH, COMMAND_TIMEOUT_MS);
            // Binder death is the primary process-termination assertion.
            fatalSession.awaitBinderDeath(CRASH_DEATH_TIMEOUT_MS);
        }
        awaitProcDisappeared(crashedPid);

        // Model the next application launch: a recovery process pointed at the same Crashpad
        // database starts the uploader for the pending fatal report.
        try (RemoteNativeServiceSession recoverySession =
                RemoteNativeServiceSession.bind(context, NativeRecoveryTestService.class, BIND_TIMEOUT_MS)) {
            prepare(recoverySession, NativeTestProtocol.CMD_PREPARE_RECOVERY, guid, databasePath);

            CoronerNativeReportAssertions.NativeReport fatalReport =
                    CoronerNativeReportAssertions.awaitExactlyOneFatal(coroner, guid, timestampStart);
            assertNotNull(fatalReport.rxid);
            logEvidence(
                    "fatal",
                    guid,
                    java.util.Collections.singletonList(fatalReport),
                    "\"crashed_pid\":" + crashedPid + ",\"binder_died\":true");

            recoverySession.request(
                    NativeTestProtocol.CMD_CLEANUP, null, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);
        }
    }

    @Test
    public void disableAndReEnableRestartsUploads() {
        String guid = UUID.randomUUID().toString();
        long timestampStart = nowSeconds() - 5;

        try (RemoteNativeServiceSession session =
                RemoteNativeServiceSession.bind(context, NativeLifecycleTestService.class, BIND_TIMEOUT_MS)) {
            prepare(session, NativeTestProtocol.CMD_PREPARE_REAL_NATIVE, guid, null);

            String beforeMessage = "before-disable-" + guid;
            Bundle beforeData = new Bundle();
            beforeData.putString(NativeTestProtocol.KEY_MESSAGE, beforeMessage);
            session.request(
                    NativeTestProtocol.CMD_DUMP, beforeData, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);
            CoronerNativeReportAssertions.awaitExactly(
                    coroner, guid, timestampStart, 1, new HashSet<>(Arrays.asList(beforeMessage)));

            session.request(
                    NativeTestProtocol.CMD_DISABLE, null, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);
            session.request(
                    NativeTestProtocol.CMD_ENABLE, null, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);

            String afterMessage = "after-reenable-" + guid;
            Bundle afterData = new Bundle();
            afterData.putString(NativeTestProtocol.KEY_MESSAGE, afterMessage);
            session.request(
                    NativeTestProtocol.CMD_DUMP, afterData, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);

            // Exactly two total reports with both distinct messages proves the upload thread
            // actually restarted, not merely that Java state changed.
            java.util.List<CoronerNativeReportAssertions.NativeReport> lifecycleReports =
                    CoronerNativeReportAssertions.awaitExactly(
                            coroner,
                            guid,
                            timestampStart,
                            2,
                            new HashSet<>(Arrays.asList(beforeMessage, afterMessage)));
            logEvidence("lifecycle", guid, lifecycleReports, null);

            session.request(
                    NativeTestProtocol.CMD_CLEANUP, null, NativeTestProtocol.EVENT_COMPLETED, COMMAND_TIMEOUT_MS);
        }
    }

    private Bundle prepare(RemoteNativeServiceSession session, int command, String guid, String databasePath) {
        Bundle data = new Bundle();
        data.putString(NativeTestProtocol.KEY_GUID, guid);
        if (databasePath != null) {
            data.putString(NativeTestProtocol.KEY_DATABASE_PATH, databasePath);
        }
        Bundle ready = session.request(command, data, NativeTestProtocol.EVENT_READY, COMMAND_TIMEOUT_MS);
        assertNotNull(ready.getString(NativeTestProtocol.KEY_HANDLER_PATH));
        assertNotNull(ready.getString(NativeTestProtocol.KEY_PROCESS_ABI));
        return ready;
    }

    /** On a split install the resolved handler path must point at an installed ABI split. */
    private void verifySplitBackedHandlerPath(Bundle ready) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo.splitSourceDirs == null || applicationInfo.splitSourceDirs.length == 0) {
            return;
        }
        String handlerPath = ready.getString(NativeTestProtocol.KEY_HANDLER_PATH);
        int separator = handlerPath.indexOf("!/");
        if (separator <= 0) {
            return; // Extracted-library install; nothing split-backed to verify.
        }
        String container = handlerPath.substring(0, separator);
        boolean installed = Arrays.asList(applicationInfo.splitSourceDirs).contains(container)
                || (applicationInfo.splitPublicSourceDirs != null
                        && Arrays.asList(applicationInfo.splitPublicSourceDirs).contains(container));
        assertTrue("Handler path is not an installed split: " + handlerPath, installed);
    }

    /** Secondary confirmation of process death; binder death is the primary assertion. */
    private static void awaitProcDisappeared(int pid) {
        long deadline = SystemClock.elapsedRealtime() + CRASH_DEATH_TIMEOUT_MS;
        File proc = new File("/proc/" + pid);
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!proc.exists()) {
                return;
            }
            SystemClock.sleep(250);
        }
        // /proc visibility can be restricted; binder death already proved termination.
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Machine-readable evidence line collected by scripts/run_split_install_test.sh into
     * native-report-evidence.json. Carries GUIDs and RXIDs only — never credentials.
     */
    private static void logEvidence(
            String phase,
            String guid,
            java.util.List<CoronerNativeReportAssertions.NativeReport> reports,
            String extraJson) {
        StringBuilder rxids = new StringBuilder("[");
        for (int i = 0; i < reports.size(); i++) {
            if (i > 0) {
                rxids.append(',');
            }
            rxids.append('"').append(reports.get(i).rxid).append('"');
        }
        rxids.append(']');
        android.util.Log.i(
                "NativeQualEvidence",
                "{\"phase\":\"" + phase + "\",\"guid\":\"" + guid + "\",\"rxids\":" + rxids + ",\"stable_count\":"
                        + reports.size() + (extraJson == null ? "" : "," + extraJson) + "}");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
