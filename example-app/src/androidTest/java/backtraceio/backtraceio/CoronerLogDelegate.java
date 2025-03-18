package backtraceio.backtraceio;

import backtraceio.coroner.common.AndroidLogDelegate;
import android.util.Log;

// Implementation that calls the real android.util.Log
public class CoronerLogDelegate implements AndroidLogDelegate {
    @Override
    public void d(String tag, String message) {
        Log.d(tag, message);
    }
}