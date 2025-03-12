package backtraceio.library.anr;

import java.io.IOException;
import java.io.InputStream;

public interface ExitInfo {
    int getReason();
    long getTimestamp();
    String getDescription();
    int getPid();
    int getUid();

    int getImportance();

    long getPss();

    long getRss();

    InputStream getTraceInputStream() throws IOException;
}
