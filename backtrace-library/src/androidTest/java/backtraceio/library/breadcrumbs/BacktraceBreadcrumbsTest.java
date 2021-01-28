package backtraceio.library.breadcrumbs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

@RunWith(AndroidJUnit4.class)
public class BacktraceBreadcrumbsTest {
    public Context context;

    static
    {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() throws IOException {
        this.context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testEnable() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);

            backtraceBreadcrumbs.enableBreadcrumbs(context);
            assertTrue(backtraceBreadcrumbs.isBreadcrumbsEnabled());

            backtraceBreadcrumbs.disableBreadcrumbs(context);
            assertFalse(backtraceBreadcrumbs.isBreadcrumbsEnabled());

        } catch(Exception e) {
            fail(e.getMessage());
        }
    }
}
