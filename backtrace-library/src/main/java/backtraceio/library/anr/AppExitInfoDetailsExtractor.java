package backtraceio.library.anr;

import android.app.ApplicationExitInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;
import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class AppExitInfoDetailsExtractor {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static String getANRMessage(ApplicationExitInfo exitInfo) {
        if (exitInfo == null) {
            return "No ApplicationExitInfo available.";
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            // TODO: log
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date(exitInfo.getTimestamp()));
        String stackTrace = AppExitInfoDetailsExtractor.getStackTraceInfo(exitInfo);
        return String.format(Locale.getDefault(),
                "App exit info: %s\n" +
                        "Timestamp: %s\n" +
                        "Reason: (code %s) %s\n" +
                        "PID: %d\n" +
                        "Importance: %d\n" +
                        "PSS (KB): %d\n" +
                        "RSS (KB): %d\n" +
                        "Stack trace: %s \n" +
                        "%s",
                exitInfo.getDescription(),
                timestamp,
                exitInfo.getReason(),
                AppExitInfoDetailsExtractor.reasonCodeToDescription(exitInfo.getReason()),
                exitInfo.getPid(),
                exitInfo.getImportance(),
                exitInfo.getPss(),
                exitInfo.getRss(),
                stackTrace != null ? "Available" : "Not Available",
                stackTrace
        );
    }

    private static String getStackTraceInfo(ApplicationExitInfo exitInfo) {
        InputStream traceStream = getStreamOrNull(exitInfo);
        if (traceStream == null) {
            // TODO: log
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(traceStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (Exception exception) {
            // TODO: log
            return "";
        }

        return builder.toString();
    }

    private static InputStream getStreamOrNull(ApplicationExitInfo exitInfo) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            return null;
        }

        try {
            return exitInfo.getTraceInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    private static String reasonCodeToDescription(int reasonCode) {
        switch(reasonCode) {
            case ApplicationExitInfo.REASON_ANR:
                return "anr";
            case ApplicationExitInfo.REASON_EXIT_SELF:
                return "exit itself";
            case ApplicationExitInfo.REASON_CRASH:
                return "crash";
            case ApplicationExitInfo.REASON_CRASH_NATIVE:
                return "crash native";
            case ApplicationExitInfo.REASON_LOW_MEMORY:
                return "low memory";
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED:
                return "dependency died";
            case ApplicationExitInfo.REASON_FREEZER:
                return "freezer";
            case ApplicationExitInfo.REASON_USER_STOPPED:
                return "user stopped";
            case ApplicationExitInfo.REASON_USER_REQUESTED:
                return "user requested";
            case ApplicationExitInfo.REASON_UNKNOWN:
                return "unknown";
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
                return "initialization failure";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                return "excessive resource usage";
            case ApplicationExitInfo.REASON_SIGNALED:
                return "signaled";
            case ApplicationExitInfo.REASON_OTHER:
                return "other";
        }
        return "unsupported code";
    }
}
