package backtraceio.library;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import backtraceio.library.common.ApplicationMetadataCache;

@RunWith(AndroidJUnit4.class)
public class ApplicationCacheTest {
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

    }

}
