package backtraceio.library.base;

public class NativeLibraryLoader {
    public static void load() {
        System.loadLibrary("backtrace-native");
    }
}
