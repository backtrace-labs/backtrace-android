package backtraceio.library.interfaces;

import java.io.IOException;

public interface IBacktraceDatabaseRecordWriter {
    String write(Object data, String prefix);
    String write(byte[] data, String prefix);
    String toJsonFile(Object data);
    void saveValidRecord(String sourcePath, String destinationPath);
    void saveTemporaryFile(String path, byte[] file) throws IOException;
}
