package backtraceio.library.breadcrumbs;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceBreadcrumbsLogger {
    /**
     * The base directory of the breadcrumb logs
     */
    final private static String breadcrumbLogDirectory = "breadcrumbs";

    final private static String breadcrumbLogFileName = "bt-breadcrumb-0";

    private final String LOG_TAG = BacktraceBreadcrumbsLogger.class.getSimpleName();

    private long breadcrumbId = 0;

    private BacktraceQueueFileHelper backtraceQueueFileHelper;

    /**
     * We truncate messages longer than this
     */
    private final int maxMessageSizeBytes = 1024;

    /**
     * We truncate serialized attribute strings longer than this
     */
    private final int maxSerializedAttributeSizeBytes = 1024;

    public BacktraceBreadcrumbsLogger(Context context, int maxQueueFileSizeBytes) throws IOException, NoSuchMethodException {
        File breadcrumbLogDir = new File(BacktraceBreadcrumbsLogger.getBreadcrumbLogDirectory(context));
        breadcrumbLogDir.mkdir();
        this.backtraceQueueFileHelper = new BacktraceQueueFileHelper(BacktraceBreadcrumbsLogger.getBreadcrumbLogPath(context), maxQueueFileSizeBytes);
    }

    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        // We use currentTimeMillis in the BacktraceReport too, so for consistency
        // we will use it here.
        long time = System.currentTimeMillis();

        message = message.substring(0, Math.min(message.length(), maxMessageSizeBytes));

        StringBuilder breadcrumb = new StringBuilder("\ntimestamp " + Long.toString(time));
        breadcrumb.append(" id ");
        breadcrumb.append(breadcrumbId);
        breadcrumb.append(" level ");
        breadcrumb.append(level.toString());
        breadcrumb.append(" type ");
        breadcrumb.append(type.toString());
        breadcrumb.append(" attributes ");
        breadcrumb.append(serializeAttributes(attributes));
        breadcrumb.append(" message ");
        breadcrumb.append(message.replace("\n", ""));
        breadcrumb.append("\n");

        breadcrumbId++;

        return backtraceQueueFileHelper.add(breadcrumb.toString().getBytes());
    }

    public boolean clear() {
        return backtraceQueueFileHelper.clear();
    }

    private String serializeAttributes(final Map<String, Object> attributes) {
        StringBuilder serializedAttributes = new StringBuilder();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey().replace(' ', '_').replace('\n', '_');
                String value = entry.getValue().toString().replace(' ', '_').replace('\n', '_');

                // We don't want to break the attributes for parsing purposes, so we
                // stop adding attributes once we are over length.
                if (serializedAttributes.length() + key.length() + value.length() <= maxSerializedAttributeSizeBytes) {
                    serializedAttributes.append(" attr ");
                    serializedAttributes.append(key);
                    serializedAttributes.append(" ");
                    serializedAttributes.append(value);
                    serializedAttributes.append(" ");
                } else {
                    BacktraceLogger.w(LOG_TAG, "Breadcrumb attributes truncated");
                    break;
                }
            }
        }
        return serializedAttributes.toString();
    }

    public long getCurrentBreadcrumbId() {
        return breadcrumbId;
    }

    public static String getBreadcrumbLogDirectory(@NonNull Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" + BacktraceBreadcrumbsLogger.breadcrumbLogDirectory;
    }

    public static String getBreadcrumbLogPath(@NonNull Context context) {
        return BacktraceBreadcrumbsLogger.getBreadcrumbLogDirectory(context) + "/" + BacktraceBreadcrumbsLogger.breadcrumbLogFileName;
    }
}
