package backtraceio.library.breadcrumbs;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceBreadcrumbsLogManager {

    private String breadcrumbLogPath;

    private final String LOG_TAG = BacktraceBreadcrumbsLogManager.class.getSimpleName();

    private long breadcrumbId = 0;

    private BacktraceQueueFileHelper backtraceQueueFileHelper;

    /**
     * We truncate messages longer than this
     */
    private final int maxMessageSizeBytes = 1024;

    /**
     * We stop adding new attributes once we hit this limit
     */
    private final int maxAttributeSizeBytes = 1024;

    public BacktraceBreadcrumbsLogManager(String breadcrumbLogPath, int maxQueueFileSizeBytes) throws IOException, NoSuchMethodException {
        this.breadcrumbLogPath = breadcrumbLogPath;
        this.backtraceQueueFileHelper = new BacktraceQueueFileHelper(this.breadcrumbLogPath, maxQueueFileSizeBytes);
    }

    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        // We use currentTimeMillis in the BacktraceReport too, so for consistency
        // we will use it here.
        long time = System.currentTimeMillis();

        message = message.substring(0, Math.min(message.length(), maxMessageSizeBytes));

        JSONObject breadcrumb = new JSONObject();
        try {
            breadcrumb.put("timestamp", time);
            breadcrumb.put("id", breadcrumbId);
            breadcrumb.put("level", level.toString());
            breadcrumb.put("type", type.toString());
            breadcrumb.put("message", message);

            if (attributes != null) {
                JSONObject attributesJson = new JSONObject();
                int currentAttributeSize = 0;
                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    currentAttributeSize += entry.getKey().length() + entry.getValue().toString().length();
                    if (currentAttributeSize < maxAttributeSizeBytes) {
                        attributesJson.put(entry.getKey(), entry.getValue());
                    }
                }
                if (attributesJson.length() > 0) {
                    breadcrumb.put("attributes", attributesJson);
                }
            }
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Could not create the breadcrumb JSON");
            return false;
        }

        // Guard the JSON with newlines so the parser can parse it from the QueueFile encoding
        StringBuilder breadcrumbSerializedString = new StringBuilder("\n");
        breadcrumbSerializedString.append(breadcrumb.toString().replace("\\n", ""));
        breadcrumbSerializedString.append("\n");

        breadcrumbId++;

        return backtraceQueueFileHelper.add(breadcrumbSerializedString.toString().getBytes());
    }

    public boolean clear() {
        return backtraceQueueFileHelper.clear();
    }

    public long getCurrentBreadcrumbId() {
        return breadcrumbId;
    }
}
