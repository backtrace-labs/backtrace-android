package backtraceio.library;

import android.content.Context;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientSendAsyncTest {
    private Context context;
    private BacktraceCredentials credentials;
    private final String resultMessage = "From request handler";
    private final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("test", "value");
    }};

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    @Test
    public void sendAsyncException() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.ServerError);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        AsyncTask<Void, Void, BacktraceResult>asyncTaskResult = backtraceClient.sendAsync(new Exception(this.resultMessage));

        BacktraceResult result = null;
        Exception error = null;
        try {
            result = asyncTaskResult.get();
        }
        catch (Exception exception)
        {
            error = exception;
        }

        // THEN
        assertNull(error);
        assertNotNull(result);
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.ServerError, result.status);
    }

    @Test
    public void sendAsyncString() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, resultMessage, BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        AsyncTask<Void, Void, BacktraceResult> asyncTaskResponse = backtraceClient.sendAsync("test");

        BacktraceResult result = null;
        Exception error = null;
        try {
            result = asyncTaskResponse.get();
        } catch (Exception e) {
            error = e;
        }

        // THEN
        assertNull(error);
        assertNotNull(result);
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
    }

}
