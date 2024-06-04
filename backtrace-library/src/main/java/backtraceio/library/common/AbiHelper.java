package backtraceio.library.common;

import android.os.Build;

public class AbiHelper {
    public static String getCurrentAbi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // on newer Android versions, we'll return only the most important Abi version
            return Build.SUPPORTED_ABIS[0];
        }
        // on pre-Lollip versions, we got only one Abi
        return Build.CPU_ABI;
    }
}
