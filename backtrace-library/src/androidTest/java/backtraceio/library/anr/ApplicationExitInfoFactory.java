package backtraceio.library.anr;

import android.app.ApplicationExitInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class ApplicationExitInfoFactory {
    public static ApplicationExitInfo createApplicationExitInfo(
            int reason, String description, int pid, long timestamp) throws Exception {

        Constructor<ApplicationExitInfo> constructor = ApplicationExitInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        ApplicationExitInfo exitInfo = constructor.newInstance();

        setField(exitInfo, "mReason", reason);
        setField(exitInfo, "mDescription", description);
        setField(exitInfo, "mPid", pid);
        setField(exitInfo, "mTimestamp", timestamp);

        return exitInfo;
    }

    private static void setField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}

