package backtraceio.library.anr;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityManagerExitInfoProvider implements ProcessExitInfoProvider {
    private final ActivityManager activityManager;
    public ActivityManagerExitInfoProvider(Context context) {
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public List<ExitInfo> getHistoricalExitInfo(String packageName, int startIndex, int maxCount) {
        List<ApplicationExitInfo> systemExitInfoList =
                activityManager.getHistoricalProcessExitReasons(packageName, startIndex, maxCount);

        if (systemExitInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExitInfo> result = new ArrayList<>(systemExitInfoList.size());
        for (ApplicationExitInfo info : systemExitInfoList) {
            result.add(new ExitInfoAdapter(info));
        }

        return result;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public List<Integer> getSupportedTypesOfExitInfo() {
        return Collections.singletonList(ApplicationExitInfo.REASON_ANR);
    }
}
