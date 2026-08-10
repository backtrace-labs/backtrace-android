package backtraceio.library.base;

/**
 * Loads the Backtrace native library and records the exact path selected by Android's native linker.
 */
public class NativeLibraryLoader {
    private static final Object LOAD_LOCK = new Object();

    private static volatile boolean loaded;
    private static volatile String loadedLibraryPath;

    public NativeLibraryLoader() {}

    public static void load() {
        if (loaded) {
            return;
        }

        synchronized (LOAD_LOCK) {
            if (loaded) {
                return;
            }

            System.loadLibrary("backtrace-native");
            loadedLibraryPath = safelyResolveLoadedLibraryPath();
            loaded = true;
        }
    }

    /**
     * Returns the absolute filesystem or APK-backed path of the loaded Backtrace native library.
     *
     * <p>The value can be {@code null} when the platform linker cannot expose module metadata.
     * A null value is non-fatal because native integration retains metadata-only fallbacks.
     */
    public static String getLoadedLibraryPath() {
        return loadedLibraryPath;
    }

    private static String safelyResolveLoadedLibraryPath() {
        try {
            return resolveLoadedLibraryPath();
        } catch (LinkageError | SecurityException ignored) {
            return null;
        }
    }

    private static native String resolveLoadedLibraryPath();
}
