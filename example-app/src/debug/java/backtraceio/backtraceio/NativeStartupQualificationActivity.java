package backtraceio.backtraceio;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug-only startup-qualification entry point: performs the production main-thread native
 * initialization inside a named trace section so Perfetto captures its exact cost, writes a small
 * result JSON to app-private storage for the capture script, then disables and finishes. No
 * tracing dependency is added to the release SDK; {@link Trace} is a platform API.
 */
public final class NativeStartupQualificationActivity extends Activity {

    private static final String TRACE_SECTION = "BacktraceQualification#tryEnableNativeIntegration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File databaseRoot = new File(getFilesDir(), "startup-qualification-" + SystemClock.elapsedRealtimeNanos());
        databaseRoot.mkdirs();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("test.native.qualification", "startup");

        String submissionUrl = BuildConfig.BACKTRACE_SUBMISSION_URL;
        boolean nativeEnabled = false;
        long startNanos = System.nanoTime();
        BacktraceClient client = null;
        try {
            if (submissionUrl != null && !submissionUrl.trim().isEmpty()) {
                BacktraceCredentials credentials = new BacktraceCredentials(submissionUrl);
                BacktraceDatabase database =
                        new BacktraceDatabase(this, new BacktraceDatabaseSettings(databaseRoot.getAbsolutePath()));
                client = new BacktraceClient(this, credentials, database, attributes, new java.util.ArrayList<>());

                Trace.beginSection(TRACE_SECTION);
                try {
                    nativeEnabled = client.tryEnableNativeIntegration();
                } finally {
                    Trace.endSection();
                }
            }
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            writeResult(nativeEnabled, durationNanos);
            if (client != null) {
                client.disableNativeIntegration();
            }
            finish();
        }
    }

    private void writeResult(boolean nativeEnabled, long durationNanos) {
        String json = "{\"trace_section\":\"" + TRACE_SECTION + "\",\"native_enabled\":" + nativeEnabled
                + ",\"duration_nanos\":" + durationNanos + "}";
        File result = new File(getFilesDir(), "native-startup-qualification.json");
        try (FileOutputStream output = new FileOutputStream(result)) {
            output.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
            // The capture script treats a missing result file as a failed iteration.
        }
    }
}
