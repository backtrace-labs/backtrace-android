package backtraceio.library.common.anr;

import android.app.ApplicationExitInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class ApplicationExitInfoFactory {
    public static ApplicationExitInfo createApplicationExitInfo(
            int reason, String description, int pid, long timestamp) throws Exception {

        // Get the constructor using reflection (it's hidden/private)
        Constructor<ApplicationExitInfo> constructor = ApplicationExitInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Create instance
        ApplicationExitInfo exitInfo = constructor.newInstance();

        // Set fields using reflection
        setField(exitInfo, "mReason", reason);
        setField(exitInfo, "mDescription", description);
        setField(exitInfo, "mPid", pid);
        setField(exitInfo, "mTimestamp", timestamp);
        // Set other fields as needed

        return exitInfo;
    }

    private static void setField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}

