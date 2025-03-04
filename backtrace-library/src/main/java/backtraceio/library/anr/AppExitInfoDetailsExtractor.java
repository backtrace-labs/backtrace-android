package backtraceio.library.anr;

import android.app.ApplicationExitInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import backtraceio.library.logger.BacktraceLogger;


public class AppExitInfoDetailsExtractor {
    private final static String LOG_TAG = AppExitInfoDetailsExtractor.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static HashMap<String, Object> getANRAttributes(ApplicationExitInfo appExitInfo) {
        if (appExitInfo == null) {
            return new HashMap<>();
        }

        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("description", appExitInfo.getDescription());
        attributes.put("timestamp", getANRTimestamp(appExitInfo));
        attributes.put("stackTrace", AppExitInfoDetailsExtractor.getStackTraceInfo(appExitInfo));
        attributes.put("reason-code", appExitInfo.getReason());
        attributes.put("reason", reasonCodeToDescription(appExitInfo.getReason()));
        attributes.put("PID", appExitInfo.getPid());
        attributes.put("Importance", appExitInfo.getImportance());
        attributes.put("PSS", appExitInfo.getPss());
        attributes.put("RSS", appExitInfo.getRss());
        return attributes;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static String getANRMessage(ApplicationExitInfo appExitInfo) {
        return "ApplicationExitInfo ANR Exception\n" +
                "Description: " + appExitInfo.getDescription() + "\n" +
                "Timestamp: " + getANRTimestamp(appExitInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static String getANRTimestamp(ApplicationExitInfo appExitInfo) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date(appExitInfo.getTimestamp()));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static String getStackTraceInfo(ApplicationExitInfo exitInfo) {
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

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static InputStream getStreamOrNull(ApplicationExitInfo exitInfo) {
        try {
            return exitInfo.getTraceInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    private static String reasonCodeToDescription(int reasonCode) {
        if (reasonCode == ApplicationExitInfo.REASON_ANR) {
            return "anr";
        }
        return "unsupported code";
    }
}
