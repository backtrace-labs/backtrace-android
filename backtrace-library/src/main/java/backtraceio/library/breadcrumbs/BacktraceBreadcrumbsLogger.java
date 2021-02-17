package backtraceio.library.breadcrumbs;

import com.squareup.tape.QueueFile;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;

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

        message = StringUtils.abbreviate(message, maxMessageSizeBytes);

        String breadcrumb = "\ntimestamp " + Long.toString(time);
        breadcrumb += " id " + Long.toString(breadcrumbId);
        breadcrumb += " level " + level.toString();
        breadcrumb += " type " + type.toString();
        breadcrumb += " attributes " + serializeAttributes(attributes);
        breadcrumb += " message " + message.replace('\n', '_');
        breadcrumb += "\n";

        breadcrumbId++;

        return backtraceQueueFileHelper.add(breadcrumb.getBytes());
    }

    public boolean clear() {
        return backtraceQueueFileHelper.clear();
    }

    public static String getLogFileName() {
        return BacktraceQueueFileHelper.getLogFileName();
    }

    private String serializeAttributes(final Map<String, Object> attributes) {
        String serializedAttributes = "";
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey().replace(' ', '_').replace('\n', '_');
                String value = entry.getValue().toString().replace(' ', '_').replace('\n', '_');

                // We don't want to break the attributes for parsing purposes, so we
                // stop adding attributes once we are over length.
                if (serializedAttributes.length() + key.length() + value.length() <= maxSerializedAttributeSizeBytes) {
                    serializedAttributes += " attr " + key + " " + value + " ";
                } else {
                    BacktraceLogger.w(LOG_TAG, "Breadcrumb attributes truncated");
                    break;
                }
            }
        }

        return serializedAttributes;
    }

    public long getCurrentBreadcrumbId() {
        return breadcrumbId;
    }
}
