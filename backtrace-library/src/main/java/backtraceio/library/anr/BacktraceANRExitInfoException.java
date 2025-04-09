package backtraceio.library.anr;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import backtraceio.library.logger.BacktraceLogger;

public class BacktraceANRExitInfoException extends Exception {
    private static String LOG_TAG = BacktraceANRExitInfoException.class.getSimpleName();
    private transient StackTraceElement[] anrStackTrace;

    @RequiresApi(api = Build.VERSION_CODES.R)
    public BacktraceANRExitInfoException(ExitInfo exitInfo) {
        super(AppExitInfoDetailsExtractor.getANRMessage(exitInfo));

        initAnrStackTrace(exitInfo);
    }

    @NonNull
    @Override
    public StackTraceElement[] getStackTrace() {
        if (anrStackTrace != null) {
            return anrStackTrace;
        }
        return super.getStackTrace();
    }

    public void initAnrStackTrace(ExitInfo exitInfo) {
        try {
            String stackTrace = getStackTraceInfo(exitInfo);
            this.anrStackTrace = ExitInfoStackTraceParser.parseMainThreadStackTrace(stackTrace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static InputStream getStreamOrNull(ExitInfo exitInfo) {
        try {
            return exitInfo.getTraceInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    private static String getStackTraceInfo(ExitInfo exitInfo) {
        InputStream traceStream = getStreamOrNull(exitInfo);
        if (traceStream == null) {
            BacktraceLogger.w(LOG_TAG, "Unexpected null trace stream");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(traceStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (Exception exception) {
            BacktraceLogger.e(LOG_TAG, "Unexpected exception on getting stacktrace from exitInfo", exception);
            return "";
        }

        return builder.toString();
    }

}
