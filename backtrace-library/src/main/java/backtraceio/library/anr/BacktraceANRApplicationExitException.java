package backtraceio.library.anr;

import android.app.ApplicationExitInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import backtraceio.library.logger.BacktraceLogger;

public class BacktraceANRApplicationExitException extends Exception {
    private final static transient String LOG_TAG = BacktraceANRApplicationExitException.class.getSimpleName();
    public BacktraceANRApplicationExitException(ApplicationExitInfo exitInfo) {
        super();

        getStackTraceInfo(exitInfo);
    }

    private void getStackTraceInfo(ApplicationExitInfo exitInfo) {
        InputStream traceStream = getStreamOrNull(exitInfo);
        if (traceStream == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(traceStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (Exception exception) {
            return;
        }

        String stackTrace = builder.toString();
        BacktraceLogger.d("F", stackTrace);
    }

    private InputStream getStreamOrNull(ApplicationExitInfo exitInfo) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            return null;
        }

        try {
            return exitInfo.getTraceInputStream();
        } catch (IOException e) {
            return null;
        }
    }
}
