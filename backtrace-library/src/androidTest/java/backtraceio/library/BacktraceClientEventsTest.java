package backtraceio.library;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class BacktraceClientEventsTest {
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
        final Waiter waiter = new Waiter();
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(null, resultMessage, BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send("test", new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                // THEN
                assertEquals(resultMessage, backtraceResult.message);
                assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
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

    @Test
    public void useBeforeSendAndRequestHandler() {
        // GIVEN
        final String attributeKey = UUID.randomUUID().toString();
        final Waiter waiter = new Waiter();

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
        backtraceClient.send("test", new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                // THEN
                waiter.assertEquals(resultMessage, backtraceResult.message);
                waiter.assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
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

    @Test
    public void useOnServerError() {
        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final ArrayList<Exception> exceptions = new ArrayList<>();
        backtraceClient.setOnServerErrorEventListener(new OnServerErrorEventListener() {
            @Override
            public void onEvent(Exception exception) {
                exceptions.add(exception);
            }
        });

        // WHEN
        backtraceClient.send(new Exception("test"), new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                // THEN
                assertEquals(1, exceptions.size());
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
