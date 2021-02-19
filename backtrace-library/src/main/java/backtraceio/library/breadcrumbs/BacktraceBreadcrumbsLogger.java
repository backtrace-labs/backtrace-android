package backtraceio.library.breadcrumbs;

import java.io.IOException;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceBreadcrumbsLogger {

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

    public BacktraceBreadcrumbsLogger(String breadcrumbLogDirectory, int maxQueueFileSizeBytes) throws IOException, NoSuchMethodException {
        this.backtraceQueueFileHelper = new BacktraceQueueFileHelper(breadcrumbLogDirectory, maxQueueFileSizeBytes);
    }

    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        // We use currentTimeMillis in the BacktraceReport too, so for consistency
        // we will use it here.
        long time = System.currentTimeMillis();

        message = message.substring(0, Math.min(message.length(), maxMessageSizeBytes));

        StringBuilder breadcrumb = new StringBuilder("\ntimestamp " + Long.toString(time));
        breadcrumb.append(" id " + Long.toString(breadcrumbId));
        breadcrumb.append(" level " + level.toString());
        breadcrumb.append(" type " + type.toString());
        breadcrumb.append(" attributes " + serializeAttributes(attributes));
        breadcrumb.append(" message " + message.replace('\n', '_'));
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
                    serializedAttributes.append(" attr " + key + " " + value + " ");
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
}
