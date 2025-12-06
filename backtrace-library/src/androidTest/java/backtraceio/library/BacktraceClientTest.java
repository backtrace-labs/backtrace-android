package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientTest {
    private final String resultMessage = "From request handler";

    private Context context;
    private BacktraceCredentials credentials;



    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }
    @Test
    public void sendBacktraceReportWithString() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();

        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertEquals(1, 2);
                assertEquals(0, data.getAttachmentPaths().size());
                waiter.resume();
                return new BacktraceResult(data.getReport(), data.getReport().message,
                        BacktraceResultStatus.Ok);
            }
        };

        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage));
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
