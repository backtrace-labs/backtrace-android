package backtraceio.library;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientAttachmentsTest {
    static {
        System.loadLibrary("backtrace-native");
    }

    private final String resultMessage = "From request handler";
    private Context context;
    private BacktraceCredentials credentials;
    private BacktraceDatabase database;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");

        database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());
    }

    @Test
    public void sendBacktraceExceptionAttachments() {
        // GIVEN
        final String attachment0 = "/someDir/someFile.log";
        final String attachment1 = "/someDir/someOtherFile.log";
        List<String> attachments = new ArrayList<String>() {{
            add(attachment0);
            add(attachment1);
        }};

        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database, attachments);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(String url, BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }

            @Override
            public BacktraceResult onNativeRequest(String url, BacktraceNativeData data) {
                return null;
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
                        assertNotNull(backtraceResult.getBacktraceReport().exception);

                        // We should have the file attachment paths included
                        assertEquals(2, backtraceResult.getBacktraceReport().attachmentPaths.size());
                        assertTrue(backtraceResult.getBacktraceReport().attachmentPaths.contains(attachment0));
                        assertTrue(backtraceResult.getBacktraceReport().attachmentPaths.contains(attachment1));

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
    public void sendBacktraceExceptionNoAttachments() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(String url, BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }

            @Override
            public BacktraceResult onNativeRequest(String url, BacktraceNativeData data) {
                return null;
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
                        assertNotNull(backtraceResult.getBacktraceReport().exception);

                        // We should NOT have any attachment paths included by default
                        assertEquals(0, backtraceResult.getBacktraceReport().attachmentPaths.size());

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
}
