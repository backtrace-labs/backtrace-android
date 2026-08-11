package backtraceio.backtraceio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.coroner.CoronerClient;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Play-style split-install qualification through the real crash-handler bridge: initializes native
 * integration against the split-backed library path via the production
 * {@code BacktraceCrashHandlerWrapper}, submits a uniquely correlated {@code dumpWithoutCrash}, and
 * requires exactly one matching report to reach the backend. This proves the crash-handler side can
 * consume the resolved split-backed path, which the resolver-only test deliberately does not cover.
 *
 * <p>Skips itself on universal or extracted installs and when Backtrace test credentials are not
 * configured, so it is safe in every lane and meaningful only in the dedicated AAB job.
 */
@RunWith(AndroidJUnit4.class)
public class SplitInstallNativeIntegrationTest {
    private static final long INGESTION_TIMEOUT_MS = 90_000;
    private static final long POLL_INTERVAL_MS = 2_000;

    @Test
    public void splitBackedHandlerLoadsAndSubmitsDumpWithoutCrash() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();

        assumeTrue(
                "Dedicated AAB split-install test requires installed splits",
                applicationInfo.splitSourceDirs != null && applicationInfo.splitSourceDirs.length > 0);
        assumeTrue(
                "Backtrace test credentials are not configured",
                hasText(BuildConfig.BACKTRACE_SUBMISSION_URL)
                        && hasText(BuildConfig.BACKTRACE_CORONER_URL)
                        && hasText(BuildConfig.BACKTRACE_CORONER_TOKEN));

        String resolved = getEnvironmentValue(
                new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertNotNull(resolved);

        int separator = resolved.indexOf("!/");
        assumeTrue("Dedicated test requires an APK-backed native library: " + resolved, separator > 0);

        String splitContainer = resolved.substring(0, separator);
        assertTrue(
                "Resolved container is not an installed split: " + resolved,
                contains(applicationInfo.splitSourceDirs, splitContainer)
                        || contains(applicationInfo.splitPublicSourceDirs, splitContainer));

        String correlationId = UUID.randomUUID().toString();
        String message = "SplitInstallDumpWithoutCrash-" + correlationId;

        File databaseRoot = new File(context.getFilesDir(), "split-native-test-" + correlationId);
        assertTrue(databaseRoot.mkdirs() || databaseRoot.isDirectory());

        BacktraceCredentials credentials = new BacktraceCredentials(BuildConfig.BACKTRACE_SUBMISSION_URL);
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(databaseRoot.getAbsolutePath());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("test.native_correlation_id", correlationId);

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        BacktraceClient client = new BacktraceClient(context, credentials, database, attributes, new ArrayList<>());

        assertTrue(
                "Native integration failed for split-backed path: " + resolved,
                database.setupNativeIntegration(client, credentials));

        long timestampStart = System.currentTimeMillis() / 1000L;
        client.dumpWithoutCrash(message);

        CoronerClient coroner =
                new CoronerClient(BuildConfig.BACKTRACE_CORONER_URL, BuildConfig.BACKTRACE_CORONER_TOKEN);
        awaitExactlyOneReport(coroner, timestampStart, message);

        client.disableNativeIntegration();
    }

    private static void awaitExactlyOneReport(CoronerClient coroner, long timestampStart, String expectedMessage) {
        long deadline = SystemClock.elapsedRealtime() + INGESTION_TIMEOUT_MS;
        Exception lastPollFailure = null;
        while (SystemClock.elapsedRealtime() < deadline) {
            // A single malformed or transient error response must not abort the poll; retry until
            // the deadline and report the last failure only when no report ever arrived.
            try {
                CoronerResponse response = coroner.errorTypeTimestampFilter(
                        "Crash",
                        Long.toString(timestampStart),
                        Long.toString(System.currentTimeMillis() / 1000L),
                        Arrays.asList("error.message"));

                int matchingReports = 0;
                for (int index = 0; index < response.getResultsNumber(); index++) {
                    String message = response.getAttribute(index, "error.message", String.class);
                    if (expectedMessage.equals(message)) {
                        matchingReports++;
                    }
                }

                if (matchingReports == 1) {
                    return;
                }
                if (matchingReports > 1) {
                    fail("Expected exactly one report for " + expectedMessage + ", found " + matchingReports);
                }
                lastPollFailure = null;
            } catch (Exception pollFailure) {
                lastPollFailure = pollFailure;
            }
            SystemClock.sleep(POLL_INTERVAL_MS);
        }
        fail("No split-backed native dump was ingested for " + expectedMessage
                + (lastPollFailure == null ? "" : "; last Coroner failure: " + lastPollFailure));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static boolean contains(String[] paths, String expected) {
        return paths != null && Arrays.asList(paths).contains(expected);
    }

    private static String getEnvironmentValue(List<String> environment, String key) {
        String prefix = key + "=";
        for (String value : environment) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return null;
    }
}
