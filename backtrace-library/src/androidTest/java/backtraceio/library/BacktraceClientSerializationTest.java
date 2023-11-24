package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.annotations.SerializedName;
import net.jodah.concurrentunit.Waiter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

abstract class CustomExceptionBase extends Exception {
    @SerializedName("message")
    public String Message;
    public CustomExceptionBase(String message) {
        super(message);
        this.Message = message;
    }
}
class SerializationTestException extends CustomExceptionBase {
    @SerializedName("message")
    public String Message;

    public SerializationTestException(String message) {
        super(message);
        // modify the exception in the source class
        this.Message = message + message;
    }
}


@RunWith(AndroidJUnit4.class)
public class BacktraceClientSerializationTest {
    private Context context;
    private BacktraceCredentials credentials;
    private final String resultMessage = "serialization test message";
    private final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("test", "value");
    }};

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }


    @Test
    public void sendReportWithTheSameJsonKeys() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                String dataJson = BacktraceSerializeHelper.toJson(data);
                assertNotNull(dataJson);
                String reportJson = BacktraceSerializeHelper.toJson(data.report);
                assertNotNull(reportJson);


                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(new SerializationTestException("serialization test message")), new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                // THEN
                assertEquals(resultMessage, backtraceResult.message);
                assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                assertNotNull(backtraceResult.getBacktraceReport());
                assertNotNull(backtraceResult.getBacktraceReport().exception);
                waiter.resume();
            }
        });
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
