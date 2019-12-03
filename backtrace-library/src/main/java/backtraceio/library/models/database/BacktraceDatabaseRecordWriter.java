package backtraceio.library.models.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.DatabaseRecordWriter;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceDatabaseRecordWriter implements DatabaseRecordWriter {

    private static transient final String LOG_TAG = BacktraceDatabaseRecordWriter.class.getSimpleName();

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
        String filename = String.format("%s.json", prefix);
        String tempFilePath = new File(this._destinationPath, String.format("temp_%s", filename)).getAbsolutePath();
        saveTemporaryFile(tempFilePath, data);

        String destFilePath = new File(this._destinationPath, filename).getAbsolutePath();
        this.saveValidRecord(tempFilePath, destFilePath);
        return destFilePath;
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

    /**
     * Save valid diagnostic data from temporary file
     *
     * @param sourcePath      temporary file path
     * @param destinationPath destination path
     * @throws IOException
     */
    private void saveValidRecord(String sourcePath, String destinationPath) throws IOException {
        File fromFile = new File(sourcePath);
        File toFile = new File(destinationPath);
        boolean renameResult = fromFile.renameTo(toFile);
        if (!renameResult) {
            BacktraceLogger.e(LOG_TAG, "Can not rename file");
            throw new IOException(String.format("Can not rename file. Source path: %s, destination path: %s", sourcePath, destinationPath));
        }
    }

    /**
     * Save temporary file to storage
     *
     * @param path path to temporary file
     * @param file current file
     * @throws IOException
     */
    private void saveTemporaryFile(String path, byte[] file) throws IOException {
        BacktraceLogger.d(LOG_TAG, "Saving temporary file");
        FileOutputStream out = new FileOutputStream(path);
        out.write(file);
        out.close();
    }
}
