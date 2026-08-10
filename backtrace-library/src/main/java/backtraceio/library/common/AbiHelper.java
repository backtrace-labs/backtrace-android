package backtraceio.library.common;

import android.os.Build;

public class AbiHelper {
    /**
     * Returns the primary ABI of the current application process.
     *
     * <p>{@link Build#SUPPORTED_ABIS} is ordered for the <em>device</em> and its first entry is 64-bit on any 64-bit-capable device, even when the current process is 32-bit.
     * {@link Build#CPU_ABI} is adjusted by Android for the bitness of the running process, so it is the correct source for a value used to locate this process's native libraries.
     */
    @SuppressWarnings("deprecation")
    public static String getCurrentAbi() {
        String processAbi = normalize(Build.CPU_ABI);
        if (processAbi != null) {
            return processAbi;
        }

        // Defensive fallback for malformed vendor builds that leave CPU_ABI empty.
        if (Build.SUPPORTED_ABIS != null) {
            for (String supportedAbi : Build.SUPPORTED_ABIS) {
                String normalizedAbi = normalize(supportedAbi);
                if (normalizedAbi != null) {
                    return normalizedAbi;
                }
            }
        }

        throw new IllegalStateException("Unable to determine the current process ABI");
    }

    private static String normalize(String abi) {
        if (abi == null) {
            return null;
        }
        String normalizedAbi = abi.trim();
        return normalizedAbi.isEmpty() ? null : normalizedAbi;
    }
}
