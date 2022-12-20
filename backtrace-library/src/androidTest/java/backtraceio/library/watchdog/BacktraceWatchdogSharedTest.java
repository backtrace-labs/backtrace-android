package backtraceio.library.watchdog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static backtraceio.library.models.BacktraceAttributeConsts.AnrAttributeType;

import android.content.Context;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.breadcrumbs.BreadcrumbsReader;
import backtraceio.library.logger.BacktraceLogger;

@RunWith(AndroidJUnit4.class)
public class BacktraceWatchdogSharedTest {

    private Context context;
    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private String dbPath;
    private BacktraceDatabase database;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.database = new BacktraceDatabase(this.context, dbPath);
        this.database.start();
        this.database.clear();
    }

    @After
    public void after() {
        this.database.clear();
    }

    @org.junit.Test
    @UiThreadTest
    public void checkAnrBreadcrumb() {
        // GIVEN
        final Waiter waiter = new Waiter();
        final BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        backtraceClient.enableBreadcrumbs(context);
        backtraceClient.clearBreadcrumbs();

        backtraceClient.setOnRequestHandler(data -> {
            String breadcrumbPath = data.report.attachmentPaths.get(0);

            assertTrue(breadcrumbPath.contains("bt-breadcrumbs"));
            assertEquals(data.attributes.get("error.type"), AnrAttributeType);
            assertEquals("ANR detected - thread is blocked", getANRBreadcrumb());

            waiter.resume();
            return null;
        });

        // WHEN
        BacktraceWatchdogShared.sendReportCauseBlockedThread(
                backtraceClient, new Thread(), null, "");

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    public String getANRBreadcrumb() {
        try {
            List<String> result = BreadcrumbsReader.readBreadcrumbLogFile(dbPath);
            JSONObject anrBreadcrumb = new JSONObject(result.get(1));
            return anrBreadcrumb.getString("message");
        } catch (Exception e) {
            BacktraceLogger.e(BacktraceWatchdogSharedTest.class.getName(),
                    "Exception on looking for ANR breadcrumb", e);
            return null;
        }
    }
}
