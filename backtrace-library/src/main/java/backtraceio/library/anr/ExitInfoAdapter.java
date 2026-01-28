package backtraceio.library.anr;

import android.app.ApplicationExitInfo;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import java.io.InputStream;

public class ExitInfoAdapter implements ExitInfo {
    private final ApplicationExitInfo systemExitInfo;

    public ExitInfoAdapter(ApplicationExitInfo systemExitInfo) {
        this.systemExitInfo = systemExitInfo;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public int getReason() {
        return systemExitInfo.getReason();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public long getTimestamp() {
        return systemExitInfo.getTimestamp();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public String getDescription() {
        return systemExitInfo.getDescription();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public int getPid() {
        return systemExitInfo.getPid();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public int getUid() {
        return systemExitInfo.getDefiningUid();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public int getImportance() {
        return systemExitInfo.getImportance();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public long getPss() {
        return systemExitInfo.getPss();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public long getRss() {
        return systemExitInfo.getRss();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public InputStream getTraceInputStream() throws IOException {
        return systemExitInfo.getTraceInputStream();
    }
}
