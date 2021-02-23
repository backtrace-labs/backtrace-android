package backtraceio.library;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientBreadcrumbsTest {
    private Context context;
    private BacktraceCredentials credentials;
    private BacktraceClient backtraceClient;
    private final String resultMessage = "From request handler";

    static
    {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");

        BacktraceDatabase database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());
        backtraceClient = new BacktraceClient(context, credentials, database);
    }

    @After
    public void cleanUp() {
        File dir = new File(context.getFilesDir().getAbsolutePath());
        deleteRecursive(dir);
    }

    @Test
    public void sendBacktraceExceptionBreadcrumbs() {
        // GIVEN
        backtraceClient.enableBreadcrumbs(context);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception(resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().attributes);
                        assertNotNull(backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));
                        assertNotNull(backtraceResult.getBacktraceReport().exception);

                        // We should have the breadcrumbs attachment path included if breadcrumbs are enabled
                        assertNotEquals(0, backtraceResult.getBacktraceReport().attachmentPaths.size());

                        // We log one breadcrumb by default, the breadcrumb configuration
                        assertEquals((long) 1,
                                backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));

                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceExceptionBreadcrumbsAddBreadcrumb() {
        // GIVEN
        backtraceClient.enableBreadcrumbs(context);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        backtraceClient.addBreadcrumb("breadcrumb");

        // WHEN
        backtraceClient.send(new Exception(resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().attributes);
                        assertNotNull(backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));
                        assertNotNull(backtraceResult.getBacktraceReport().exception);

                        // We should have the breadcrumbs attachment path included if breadcrumbs are enabled
                        assertNotEquals(0, backtraceResult.getBacktraceReport().attachmentPaths.size());

                        // After adding a new breadcrumb, we should have incremented the breadcrumbs.lastId
                        assertEquals((long) 2,
                                backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));

                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceExceptionNoBreadcrumbs() {
        // GIVEN
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception(resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().attributes);
                        assertNotNull(backtraceResult.getBacktraceReport().exception);

                        // We should NOT have the breadcrumbs attachment path included by default
                        assertEquals(0, backtraceResult.getBacktraceReport().attachmentPaths.size());

                        assertNull(backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }
}
