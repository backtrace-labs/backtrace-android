package backtraceio.library.anr;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class BacktraceANRExitInfoException extends Exception {
    private final transient StackTraceElement[] anrStackTrace;

    @RequiresApi(api = Build.VERSION_CODES.R)
    public BacktraceANRExitInfoException(ExitInfo exitInfo, StackTraceElement[] stackTraceElements) {
        super(AppExitInfoDetailsExtractor.getANRMessage(exitInfo));

        this.anrStackTrace = stackTraceElements;
    }

    @NonNull
    @Override
    public StackTraceElement[] getStackTrace() {
        if (anrStackTrace != null) {
            return anrStackTrace;
        }
        return super.getStackTrace();
    }
}
