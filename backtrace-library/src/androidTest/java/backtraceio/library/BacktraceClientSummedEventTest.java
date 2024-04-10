package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.UnsupportedMetricsServer;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.EventsResult;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceMetrics;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientSummedEventTest {
    public Context context;
    public BacktraceClient backtraceClient;
    public BacktraceCredentials credentials;
    private final String summedEventName = "activity-changed";

    private final String defaultBaseUrl = BacktraceMetrics.defaultBaseUrl;
    private final String token = "aaaaabbbbbccccf82668682e69f59b38e0a853bed941e08e85f4bf5eb2c5458";

    /**
     * NOTE: Some of these tests are very time-sensitive so you may occasionally get false negative results.
     * For best results run under low CPU load and low memory utilization conditions.
     */

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://universe.sp.backtrace.io:6098", token);
        BacktraceDatabase database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());

        backtraceClient = new BacktraceClient(context, credentials, database);

        BacktraceLogger.setLevel(LogLevel.DEBUG);
    }

    public class MockRequestHandler implements EventsRequestHandler {
        public int numAttempts = 0;
        public int statusCode = 200;
        public String lastEventPayloadJson;

        @Override
        public EventsResult onRequest(EventsPayload data) {
            String eventsPayloadJson = BacktraceSerializeHelper.toJson(data);
            lastEventPayloadJson = eventsPayloadJson;
            numAttempts++;

            BacktraceResultStatus status;
            if (statusCode == 200) {
                status = BacktraceResultStatus.Ok;
            } else {
                status = BacktraceResultStatus.ServerError;
            }
            return new EventsResult(data, eventsPayloadJson, status, statusCode);
        }
    }

    @Test
    public void uploadSummedEventsManual() throws UnsupportedMetricsServer {
        final Waiter waiter = new Waiter();

        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setSummedEventsRequestHandler(mockRequestHandler);

        backtraceClient.getMetrics().setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.getMetrics().addSummedEvent(summedEventName);
        backtraceClient.getMetrics().send();

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        assertEquals(0, backtraceClient.getMetrics().getSummedEvents().size());
    }

    @Test
    public void doNotUploadWhenNoEventsAvailable() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setSummedEventsRequestHandler(mockRequestHandler);

        backtraceClient.getMetrics().setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                fail("Should not upload event");
            }
        });

        // When no events in queue request handler should not be called
        backtraceClient.getMetrics().send();
        assertEquals(0, mockRequestHandler.numAttempts);
    }

    @Test
    public void try3TimesOn503AndDropSummedEventsIfMaxNumEventsReached() throws UnsupportedMetricsServer {
        final Waiter waiter = new Waiter();

        final int timeBetweenRetriesMillis = 1;
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0, timeBetweenRetriesMillis));
        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.statusCode = 503;
        backtraceClient.getMetrics().setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.getMetrics().setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(mockSummedRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });

        backtraceClient.getMetrics().addSummedEvent(summedEventName);
        backtraceClient.getMetrics().addSummedEvent(summedEventName);

        assertEquals(2, backtraceClient.getMetrics().getSummedEvents().size());
        backtraceClient.getMetrics().setMaximumNumberOfEvents(1);
        backtraceClient.getMetrics().send();

        try {
            waiter.await(5, TimeUnit.SECONDS, 3);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(3, mockSummedRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());
        // We should drop summed event since we failed to send and maximum number of events is too small
        assertEquals(0, backtraceClient.getMetrics().getSummedEvents().size());
    }

    @Test
    public void addAndStoreSummedEvent() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.getMetrics().addSummedEvent(summedEventName));
        assertEquals(1, backtraceClient.getMetrics().getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.getMetrics().getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getSummedEvents().getFirst().getTimestamp());
    }

    @Test
    public void addSummedEventWithAttributes() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.getMetrics().addSummedEvent(summedEventName, myCustomAttributes));
        assertEquals(1, backtraceClient.getMetrics().getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.getMetrics().getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getSummedEvents().getFirst().getTimestamp());
        assertEquals("bar", backtraceClient.getMetrics().getSummedEvents().getFirst().getAttributes().get("foo"));
    }

    @Test
    public void shouldNotAddNullSummedEvent() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.getMetrics().addSummedEvent(null));
        assertEquals(0, backtraceClient.getMetrics().getSummedEvents().size());
    }

    @Test
    public void shouldNotAddEmptySummedEvent() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.getMetrics().addSummedEvent(""));
        assertEquals(0, backtraceClient.getMetrics().getSummedEvents().size());
    }
}
