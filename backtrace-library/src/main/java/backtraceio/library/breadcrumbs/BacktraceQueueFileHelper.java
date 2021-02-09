package backtraceio.library.breadcrumbs;

import com.squareup.tape.QueueFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceQueueFileHelper {
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

    public BacktraceQueueFileHelper(String breadcrumbLogDirectory) throws IOException {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
        File breadcrumbLogsDir = new File(breadcrumbLogDirectory);
        breadcrumbLogsDir.mkdir();
        breadcrumbStore = new QueueFile(new File(breadcrumbLogsDir + "/" + logFileName));
    }


    public boolean add(byte[] bytes) {
        try {
            breadcrumbStore.add(bytes);
        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Breadcrumb: " + new String(bytes, StandardCharsets.UTF_8) +
                    "\nCould not be added due to " + ex.getMessage());
            return false;
        }
        return true;
    }

    public String getLogDirectory() {
        return this.breadcrumbLogDirectory;
    }
}
