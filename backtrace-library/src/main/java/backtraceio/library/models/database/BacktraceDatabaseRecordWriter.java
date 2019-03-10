package backtraceio.library.models.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.IBacktraceDatabaseRecordWriter;

public class BacktraceDatabaseRecordWriter implements IBacktraceDatabaseRecordWriter {
    private final String _destinationPath;

    public BacktraceDatabaseRecordWriter(String path){
        this._destinationPath = path;
    }

    public String write(Object data, String prefix) {
        String json = toJsonFile(data);

        byte[] file = json.getBytes(StandardCharsets.UTF_8);
        return write(file, prefix);
    }

    public String write(byte[] data, String prefix) {
        String filename = String.format("%s.json", prefix);
        String tempFilePath = ""; // TODO: fix  Path.Combine(_destinationPath, $"temp_{filename}");
        saveTemporaryFile(tempFilePath, data);

        String destFilePath = ""; // TODO: fix Path.Combine(_destinationPath, $"temp_{filename}");
        this.saveValidRecord(tempFilePath, destFilePath);
        return destFilePath;
    }

    public String toJsonFile(Object data) {
        if (data == null)
        {
            return "";
        }
        return BacktraceSerializeHelper.toJson(data);
    }

    public void saveValidRecord(String sourcePath, String destinationPath) {
        File fromFile = new File(sourcePath);
        File toFile = new File(destinationPath);
        fromFile.renameTo(toFile);
    }

    public void saveTemporaryFile(String path, byte[] file) throws IOException {
        FileOutputStream out = new FileOutputStream(path);
        out.write(file);
        out.close();
    }
}
