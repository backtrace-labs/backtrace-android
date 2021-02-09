package backtraceio.library.breadcrumbs;

import com.squareup.tape.QueueFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;

public class BacktraceBreadcrumbsLogger {
    /**
     * The base directory of the breadcrumb logs
     */
    private String breadcrumbLogDirectory;

    /**
     * The breadcrumb storage file
     */
    private QueueFile breadcrumbStore;

    private String logFileName = "bt-breadcrumbs-0";

    private final String LOG_TAG = BacktraceBreadcrumbs.class.getSimpleName();

    private long breadcrumbId = 0;

    public BacktraceBreadcrumbsLogger(String breadcrumbLogDirectory) throws IOException {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
        File breadcrumbLogsDir = new File(breadcrumbLogDirectory);
        breadcrumbLogsDir.mkdir();
        breadcrumbStore = new QueueFile(new File(breadcrumbLogsDir + "/" + logFileName));
    }

    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        try {
            // We use currentTimeMillis in the BacktraceReport too, so for consistency
            // we will use it here.
            long time = System.currentTimeMillis();

            String breadcrumb = "\ntimestamp " + Long.toString(time);
            breadcrumb += " id " + Long.toString(breadcrumbId);
            breadcrumb += " level " + level.toString();
            breadcrumb += " type " + type.toString();
            breadcrumb += " attributes " + serializeAttributes(attributes);
            breadcrumb += " message " + message.replace("\n", "");
            breadcrumb += "\n";

            breadcrumbId++;

            breadcrumbStore.add(breadcrumb.getBytes());

        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Breadcrumb with message " + message +
                        " could not be added due to " + ex.getMessage());
            return false;
        }
        return true;
    }

    public String getLogDirectory() {
        return this.breadcrumbLogDirectory;
    }

    private String serializeAttributes(final Map<String, Object> attributes) {
        String serializedAttributes = "";
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                serializedAttributes += " attr " +
                        entry.getKey().replace(' ', '_').replace("\n", "") +
                        " " +
                        entry.getValue().toString().replace(' ', '_').replace("\n", "") +
                        " ";
            }
        }

        return serializedAttributes;
    }

    public long getCurrentBreadcrumbId() {
        return breadcrumbId;
    }
}
