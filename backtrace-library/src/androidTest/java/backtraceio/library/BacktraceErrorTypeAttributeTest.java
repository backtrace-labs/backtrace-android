package backtraceio.library;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceErrorTypeAttributeTest {

    private Context context;
    private BacktraceCredentials credentials;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    @Test
    public void setDefaultErrorTypeForExceptionTypeOfError() {
        BacktraceReport report = new BacktraceReport(new Exception("test exception"));

        assertTrue(report.attributes.containsKey(BacktraceAttributeConsts.ErrorType));
        assertEquals(report.attributes.get(BacktraceAttributeConsts.ErrorType), BacktraceAttributeConsts.HandledExceptionAttributeType);
    }

    @Test
    public void setDefaultErrorTypeForMessageType() {
        BacktraceReport report = new BacktraceReport("test message");

        assertTrue(report.attributes.containsKey(BacktraceAttributeConsts.ErrorType));
        assertEquals(report.attributes.get(BacktraceAttributeConsts.ErrorType), BacktraceAttributeConsts.MessageAttributeType);
    }

    @Test
    public void sendBacktraceExceptionWithErrorType() {

        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new TestRequestHandler() {
            @Override
            public BacktraceResult onRequest(String url, BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception("test exception"), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        String errorType = backtraceResult.getBacktraceReport().attributes.get(BacktraceAttributeConsts.ErrorType).toString();
                        assertEquals(
                                errorType,
                                BacktraceAttributeConsts.HandledExceptionAttributeType);

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
