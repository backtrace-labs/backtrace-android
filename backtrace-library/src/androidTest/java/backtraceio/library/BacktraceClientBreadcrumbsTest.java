package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.breadcrumbs.BacktraceBreadcrumbs;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientBreadcrumbsTest {
    private Context context;
    private BacktraceCredentials credentials;
    private BacktraceClient backtraceClient;
    private final String resultMessage = "From request handler";

    static {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
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
        // Account for mandatory configuration breadcrumb
        backtraceClient.database.getBreadcrumbs().setCurrentBreadcrumbId(1);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.getReport(), data.getReport().exception.getMessage(),
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
                        assertEquals(1L,
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
        // Account for mandatory configuration breadcrumb
        backtraceClient.database.getBreadcrumbs().setCurrentBreadcrumbId(1);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.getReport(), data.getReport().exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            backtraceClient.addBreadcrumb("breadcrumb");
            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();
            TestCase.assertEquals(2, breadcrumbLogFileData.size());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            TestCase.assertEquals("breadcrumb", parsedBreadcrumb.get("message"));

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
                            assertEquals(2L,
                                    backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));

                            waiter.resume();
                        }
                    }
            );
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceExceptionBreadcrumbsClearBreadcrumb() {
        // GIVEN
        backtraceClient.enableBreadcrumbs(context);
        // Account for mandatory configuration breadcrumb
        backtraceClient.database.getBreadcrumbs().setCurrentBreadcrumbId(1);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.getReport(), data.getReport().exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            backtraceClient.addBreadcrumb("breadcrumb");
            backtraceClient.addBreadcrumb("breadcrumb2");

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

                            // After adding new breadcrumbs, we should have incremented the breadcrumbs.lastId
                            assertEquals(3L,
                                    (long) backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));

                            waiter.resume();
                        }
                    }
            );

            List<String> breadcrumbLogFileData = readBreadcrumbLogFile();
            TestCase.assertEquals(3, breadcrumbLogFileData.size());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));
            TestCase.assertEquals("breadcrumb", parsedBreadcrumb.get("message"));

            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(2));
            TestCase.assertEquals("breadcrumb2", parsedBreadcrumb.get("message"));

            backtraceClient.clearBreadcrumbs();

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

                            // Since we cleared, we should only have the configuration breadcrumb
                            assertEquals(1L,
                                    backtraceResult.getBacktraceReport().attributes.get("breadcrumbs.lastId"));

                            waiter.resume();
                        }
                    }
            );

            breadcrumbLogFileData = readBreadcrumbLogFile();
            TestCase.assertEquals(1, breadcrumbLogFileData.size());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));
            TestCase.assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

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
                return new BacktraceResult(data.getReport(), data.getReport().exception.getMessage(),
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

    @Test
    public void verifyBreadcrumbCallbackInvocation() {
        backtraceClient.enableBreadcrumbs(context);
        Breadcrumbs breadcrumbs = backtraceClient.database.getBreadcrumbs();

        breadcrumbs.setOnSuccessfulBreadcrumbAddEventListener(breadcrumbId -> {
            assertEquals(breadcrumbs.getCurrentBreadcrumbId(), breadcrumbId);
            return;
        });

        breadcrumbs.addBreadcrumb("test");
    }

    public List<String> readBreadcrumbLogFile() throws IOException {
        BacktraceBreadcrumbs breadcrumbs = new BacktraceBreadcrumbs(context.getFilesDir().getAbsolutePath());
        File breadcrumbLogFile = new File(breadcrumbs.getBreadcrumbLogPath());

        List<String> breadcrumbLogFileData = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(breadcrumbLogFile.getAbsolutePath());

        // The encoding contains headers for the encoded data
        // We just throw away lines that don't start with "timestamp
        StringBuilder stringBuilder = new StringBuilder();
        while (inputStream.available() > 0) {
            char c = (char) inputStream.read();
            if (c == '\n') {
                String line = stringBuilder.toString();
                if (line.matches(".*timestamp.*")) {
                    breadcrumbLogFileData.add(line);
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder.append(c);
        }

        return breadcrumbLogFileData;
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
