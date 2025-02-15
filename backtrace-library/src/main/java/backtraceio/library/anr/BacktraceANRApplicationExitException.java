package backtraceio.library.anr;

import android.app.ApplicationExitInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class BacktraceANRApplicationExitException extends Exception {
    private final static transient String LOG_TAG = BacktraceANRApplicationExitException.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.R)
    public BacktraceANRApplicationExitException(ApplicationExitInfo exitInfo) {
        super(AppExitInfoDetailsExtractor.getANRMessage(exitInfo));
    }
}
