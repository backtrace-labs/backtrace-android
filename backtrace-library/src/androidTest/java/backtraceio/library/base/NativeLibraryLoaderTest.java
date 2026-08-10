package backtraceio.library.base;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NativeLibraryLoaderTest {

    @Test
    public void resolvesPathOfLoadedBacktraceLibrary() {
        NativeLibraryLoader.load();

        String loadedLibraryPath = NativeLibraryLoader.getLoadedLibraryPath();

        assertNotNull(loadedLibraryPath);
        assertTrue(loadedLibraryPath.contains("libbacktrace-native.so"));
    }
}
