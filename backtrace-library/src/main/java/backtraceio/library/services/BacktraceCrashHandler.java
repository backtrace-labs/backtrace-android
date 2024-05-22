package backtraceio.library.services;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class BacktraceCrashHandler {
   public static native void handleCrash(String libraryPath, String[] args);
    private static final String LOG_TAG = BacktraceCrashHandler.class.getSimpleName();

    public static void main(String[] args) throws IOException {
        Log.i(LOG_TAG, "Backtrace:: Crashpad crash handler execution starts");


        String path = "/data/user/0/backtraceio.backtraceio/files/temporary-test-file";


        FileOutputStream fos = new FileOutputStream(path);
        byte[] buffer = "This will be writtent in test.txt".getBytes();
        fos.write(buffer, 0, buffer.length);
        fos.close();


       String libraryPath = args[1];
       String[] cleanedArgs = new String[args.length - 1];
       cleanedArgs[0] = args[0];
       for (int i = 2; i < args.length; i++) {
           cleanedArgs[i -1] = args[i];
       }
       try {
//            BacktraceLogger.d(LOG_TAG, "Backtrace:: Loading library");
           System.load(libraryPath + "backtrace-native.so");
       } catch (UnsatisfiedLinkError e) {
           throw new RuntimeException(e);
       }

//        BacktraceLogger.d(LOG_TAG, "Backtrace:: Handling crash");
       handleCrash(libraryPath, cleanedArgs);
    }
}
