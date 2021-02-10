package backtraceio.library.breadcrumbs;

import com.squareup.tape.QueueFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

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

    private int maxQueueFileSizeBytes;

    private Method usedBytes;

    public BacktraceQueueFileHelper(String breadcrumbLogDirectory, int maxQueueFileSizeBytes) throws IOException, NoSuchMethodException {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
        File breadcrumbLogsDir = new File(breadcrumbLogDirectory);
        breadcrumbLogsDir.mkdir();
        breadcrumbStore = new QueueFile(new File(breadcrumbLogsDir + "/" + logFileName));

        usedBytes = QueueFile.class.getDeclaredMethod("usedBytes");
        usedBytes.setAccessible(true);

        // This minimum file size comes from QueueFile::INITIAL_LENGTH
        if (maxQueueFileSizeBytes < 4096) {
            this.maxQueueFileSizeBytes = 4096;
        } else {
            this.maxQueueFileSizeBytes = maxQueueFileSizeBytes;
        }
    }

    public boolean add(byte[] bytes) {
        try {
            int usedBytes = (int) this.usedBytes.invoke(breadcrumbStore);
            int breadcrumbLength = bytes.length;

            if (breadcrumbLength > 4096) {
                BacktraceLogger.e(LOG_TAG, "We should not have a breadcrumb this big, this is a bug!");
            }

            // We clear the space we need from the QueueFile first to prevent
            // the QueueFile from expanding to accommodate the new breadcrumb
            while (!breadcrumbStore.isEmpty() && (usedBytes + breadcrumbLength) > maxQueueFileSizeBytes) {
                breadcrumbStore.remove();
                usedBytes--;
            }

            breadcrumbStore.add(bytes);
        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Exception: " + ex.getMessage() +
                    "\nWhen adding breadcrumb: "  + new String(bytes, StandardCharsets.UTF_8));
            return false;
        }

        return true;
    }

    public String getLogDirectory() {
        return this.breadcrumbLogDirectory;
    }
}
