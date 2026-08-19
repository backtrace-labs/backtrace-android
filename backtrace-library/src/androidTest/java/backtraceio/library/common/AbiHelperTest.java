package backtraceio.library.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.os.Build;
import android.os.Process;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AbiHelperTest {

    @Test
    public void currentAbiIsSupportedByTheDevice() {
        String currentAbi = AbiHelper.getCurrentAbi();

        assertNotNull(currentAbi);
        assertTrue("ABI must not be blank", !currentAbi.trim().isEmpty());
        assertTrue(
                currentAbi + " not in " + Arrays.toString(Build.SUPPORTED_ABIS),
                Arrays.asList(Build.SUPPORTED_ABIS).contains(currentAbi));
    }

    /**
     * The resolved ABI must describe the bitness of <em>this process</em>, not the device's preferred ABI.
     * {@code Build.SUPPORTED_ABIS} is ordered by device preference and does not necessarily identify the current process,
     * so in a 32-bit process on a 64-bit-capable device its first entry can name an ABI the process cannot load.
     */
    @Test
    public void currentAbiMatchesProcessBitnessRatherThanDevicePreferredAbi() {
        assumeTrue("Process.is64Bit() requires API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);

        String currentAbi = AbiHelper.getCurrentAbi();
        String[] expectedAbis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;

        assertTrue(
                "process is64Bit=" + Process.is64Bit() + " but ABI " + currentAbi + " is not in "
                        + Arrays.toString(expectedAbis),
                Arrays.asList(expectedAbis).contains(currentAbi));
    }
}
