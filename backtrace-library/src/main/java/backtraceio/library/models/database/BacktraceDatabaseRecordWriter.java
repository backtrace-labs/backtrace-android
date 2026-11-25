package backtraceio.library.models.database;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.DatabaseRecordWriter;
import backtraceio.library.logger.BacktraceLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BacktraceDatabaseRecordWriter implements DatabaseRecordWriter {

    private static final transient String LOG_TAG = BacktraceDatabaseRecordWriter.class.getSimpleName();

    /**
     * Path to destination directory
     */
    private final String _destinationPath;

    /**
     * Initialize new database record writer
     *
     * @param path path to destination folder
     */
    public BacktraceDatabaseRecordWriter(String path) {
        this._destinationPath = path;
    }

    public String write(Object data, String prefix) throws IOException {
        String json = toJsonFile(data);

        byte[] file = json.getBytes(StandardCharsets.UTF_8);
        return write(file, prefix);
    }

    public String write(byte[] data, String prefix) throws IOException {
        File directory = new File(this._destinationPath);
        File destination = new File(directory, String.format("%s.json", prefix));
        File temporary = File.createTempFile("temp_", ".tmp", directory);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(data);
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Cannot finalize database record file");
            }
            return destination.getAbsolutePath();
        } catch (IOException | RuntimeException | Error writeFailure) {
            discardTemporaryFile(temporary, writeFailure);
            throw writeFailure;
        }
    }

    private static void discardTemporaryFile(File temporary, Throwable writeFailure) {
        try {
            if (temporary.delete() || !temporary.exists()) {
                return;
            }
        } catch (SecurityException ignored) {
            // Preserve the primary write failure and avoid copying paths into diagnostics.
        }
        writeFailure.addSuppressed(new IOException("Cannot remove temporary database record file"));
    }

    /**
     * Serialize object
     *
     * @param data object that will be serialized
     * @return serialized object in JSON string
     */
    private String toJsonFile(Object data) {
        if (data == null) {
            BacktraceLogger.w(LOG_TAG, "Passed object to serialization is null");
            return "";
        }
        return BacktraceSerializeHelper.toJson(data);
    }
}
