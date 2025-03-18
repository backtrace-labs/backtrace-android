package backtraceio.library.anr;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class BacktraceANRExitInfoException extends Exception {

    @RequiresApi(api = Build.VERSION_CODES.R)
    public BacktraceANRExitInfoException(ExitInfo exitInfo) {
        super(AppExitInfoDetailsExtractor.getANRMessage(exitInfo));
    }
}
