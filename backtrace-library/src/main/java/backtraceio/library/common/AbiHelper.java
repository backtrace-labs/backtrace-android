package backtraceio.library.common;

import android.os.Build;
import android.os.Process;

public class AbiHelper {
    /**
     * Returns the primary ABI of the current application process.
     *
     * <p>{@link Build#SUPPORTED_ABIS} is ordered by device preference and does not necessarily identify the ABI of the current process.
     * {@link Build#CPU_ABI} is adjusted by Android for the bitness of the running process, so it is the correct source for a value used to locate this process's native libraries.
     */
    @SuppressWarnings("deprecation")
    public static String getCurrentAbi() {
        String processAbi = normalize(Build.CPU_ABI);
        if (processAbi != null) {
            return processAbi;
        }

        // Defensive fallbacks for malformed vendor builds that leave CPU_ABI empty.
        // On API 23 and later the fallback preserves process bitness via Process.is64Bit(); 
        // API 21-22 can only use the device-ordered list, which may not reflect the current process.
        String bitnessAbi = firstValidAbi(getProcessBitnessAbis());
        if (bitnessAbi != null) {
            return bitnessAbi;
        }

        String supportedAbi = firstValidAbi(Build.SUPPORTED_ABIS);
        if (supportedAbi != null) {
            return supportedAbi;
        }

        throw new IllegalStateException("Unable to determine the current process ABI");
    }

    private static String[] getProcessBitnessAbis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null;
        }
        return Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
    }

    private static String firstValidAbi(String[] abis) {
        if (abis == null) {
            return null;
        }
        for (String abi : abis) {
            String normalizedAbi = normalize(abi);
            if (normalizedAbi != null) {
                return normalizedAbi;
            }
        }
        return null;
    }

    private static String normalize(String abi) {
        if (abi == null) {
            return null;
        }
        String normalizedAbi = abi.trim();
        if (normalizedAbi.isEmpty() || Build.UNKNOWN.equals(normalizedAbi)) {
            return null;
        }
        return normalizedAbi;
    }
}
