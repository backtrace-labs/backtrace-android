package backtraceio.library;

import android.content.Context;
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
public class BacktraceClientSendTest {
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
    public void sendException() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(null, data.report.exception.getMessage(),
                        BacktraceResultStatus.ServerError);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send(new Exception(this.resultMessage));

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.ServerError, result.status);
    }

    @Test
    public void sendBacktraceReportWithString() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send(new BacktraceReport(this.resultMessage));

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertNotNull(result.getBacktraceReport());
        assertNull(result.getBacktraceReport().exception);
    }

    @Test
    public void sendBacktraceReportWithStringAndAttributes() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send(new BacktraceReport(this.resultMessage,
                this.attributes));

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(this.attributes.get("test"),
                result.getBacktraceReport().attributes.get("test")
        );
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertNotNull(result.getBacktraceReport());
        assertNull(result.getBacktraceReport().exception);
    }

    @Test
    public void sendBacktraceReportWithException() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send(new BacktraceReport(new Exception(this
                .resultMessage)));

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertNotNull(result.getBacktraceReport());
        assertNotNull(result.getBacktraceReport().exception);
    }

    @Test
    public void sendBacktraceReportWithExceptionAndAttributes() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send(new BacktraceReport(
                new Exception(this.resultMessage), this.attributes)
        );

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals(this.attributes.get("test"),
                result.getBacktraceReport().attributes.get("test")
        );
        assertNotNull(result.getBacktraceReport());
        assertNotNull(result.getBacktraceReport().exception);
    }
}
