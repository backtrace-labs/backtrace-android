package backtraceio.library.interfaces;

import java.io.IOException;

public interface IBacktraceDatabaseRecordWriter {
    String write(Object data, String prefix) throws IOException;

    String write(byte[] data, String prefix) throws IOException;
}
