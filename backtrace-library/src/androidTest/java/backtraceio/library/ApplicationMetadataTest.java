package backtraceio.library;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.common.ApplicationMetadataCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ApplicationMetadataTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        // prepare instance
        ApplicationMetadataCache.getInstance(context);
    }

    @Test
    public void shouldCorrectlyRetrieveApplicationName() {
        ApplicationMetadataCache cache = ApplicationMetadataCache.getInstance(context);
        assertEquals(cache.getApplicationName(), context.getOpPackageName());
    }

    @Test
    public void shouldCorrectlyRetrieveApplicationPackageName() {
        ApplicationMetadataCache cache = ApplicationMetadataCache.getInstance(context);
        assertEquals(cache.getPackageName(), context.getOpPackageName());
    }

    @Test
    public void shouldCorrectlyRetrieveApplicationVersion() throws PackageManager.NameNotFoundException {
        ApplicationMetadataCache cache = ApplicationMetadataCache.getInstance(context);
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getOpPackageName(), 0);
        assertEquals(cache.getApplicationVersion(), String.valueOf(packageInfo.versionCode));
    }
}
