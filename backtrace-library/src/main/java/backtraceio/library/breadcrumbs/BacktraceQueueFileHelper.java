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
    private final String breadcrumbLogDirectory;

    /**
     * The breadcrumb storage file
     */
    private final QueueFile breadcrumbStore;

    private final String LOG_TAG = BacktraceQueueFileHelper.class.getSimpleName();

    private final int maxQueueFileSizeBytes;

    // This minimum file size comes from QueueFile::INITIAL_LENGTH
    private final int minimumQueueFileSizeBytes = 4096;

    private final Method usedBytes;

    // Let our exceptions bubble all the way up to BacktraceBreadcrumbsLogManager constructor
    // We definitely cannot construct BacktraceBreadcrumbsLogManager without an open file log
    public BacktraceQueueFileHelper(String breadcrumbLogDirectory, int maxQueueFileSizeBytes) throws IOException, NoSuchMethodException {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
        breadcrumbStore = new QueueFile(new File(this.breadcrumbLogDirectory));

        // QueueFile pre-allocates a file of a certain size and fills with empty data,
        // so normal File operations will not give us an accurate count of the bytes
        // in the file. Therefore we expose the private method QueueFile::usedBytes.
        usedBytes = QueueFile.class.getDeclaredMethod("usedBytes");
        usedBytes.setAccessible(true);

        if (maxQueueFileSizeBytes < minimumQueueFileSizeBytes) {
            this.maxQueueFileSizeBytes = minimumQueueFileSizeBytes;
        } else {
            this.maxQueueFileSizeBytes = maxQueueFileSizeBytes;
        }
    }

    public boolean add(byte[] bytes) {
        try {
            int breadcrumbLength = bytes.length;

            if (breadcrumbLength > 4096) {
                BacktraceLogger.e(LOG_TAG, "We should not have a breadcrumb this big, this is a bug!");
                return false;
            }

            // We clear the space we need from the QueueFile first to prevent
            // the QueueFile from expanding to accommodate the new breadcrumb
            // Note!
            // In the 1.2.3 version we use, the usedBytes is int, not long, and the 
            // implementation is synchronized thus safe for multithreaded access.
            // see https://github.com/square/tape/blob/tape-parent-1.2.3/tape/src/main/java/com/squareup/tape/QueueFile.java
            int usedBytes = (int) this.usedBytes.invoke(breadcrumbStore);
            while (!breadcrumbStore.isEmpty() && (usedBytes + breadcrumbLength) > maxQueueFileSizeBytes) {
                breadcrumbStore.remove();
                usedBytes = (int) this.usedBytes.invoke(breadcrumbStore);
            }

            breadcrumbStore.add(bytes);
        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Exception: " + ex.getMessage() +
                    "\nWhen adding breadcrumb: " + new String(bytes, StandardCharsets.UTF_8));
            return false;
        }

        return true;
    }

    public boolean clear() {
        try {
            breadcrumbStore.clear();
        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Exception: " + ex.getMessage() +
                    "\nWhen clearing breadcrumbs");
            return false;
        }
        return true;
    }
}
