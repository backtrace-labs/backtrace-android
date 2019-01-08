package backtraceio.library;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.UUID;

import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;


import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class EventsInstrumentedTest {
    private Context context;
    private BacktraceCredentials credentials;
    private final String resultMessage = "From request handler";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    @Test
    public void useRequestHandler() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(null, resultMessage, BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        BacktraceResult result = backtraceClient.send("test");

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
    }

    @Test
    public void useBeforeSendAndRequestHandler() {
        // GIVEN
        final String attributeKey = UUID.randomUUID().toString();

        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(null, data.attributes.get(attributeKey).toString(),
                        BacktraceResultStatus.Ok);
            }
        };

        backtraceClient.setOnRequestHandler(rh);
        backtraceClient.setOnBeforeSendEventListener(new OnBeforeSendEventListener() {
            @Override
            public BacktraceData onEvent(BacktraceData data) {
                data.attributes.put(attributeKey, resultMessage);
                return data;
            }
        });

        // WHEN
        BacktraceResult result = backtraceClient.send("test");

        // THEN
        assertEquals(resultMessage, result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
    }

    @Test
    public void useOnServerError() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final ArrayList<Exception> exceptions = new ArrayList<>();
        backtraceClient.setOnServerErrorEventListener(new OnServerErrorEventListener() {
            @Override
            public void onEvent(Exception exception) {
                exceptions.add(exception);
            }
        });

        // WHEN
        backtraceClient.send(new Exception("test"));

        // THEN
        assertEquals(1, exceptions.size());
    }
}
