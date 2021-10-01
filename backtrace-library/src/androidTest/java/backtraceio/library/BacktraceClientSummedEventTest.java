package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.metrics.BacktraceMetrics;
import backtraceio.library.metrics.EventsOnServerResponseEventListener;
import backtraceio.library.metrics.EventsPayload;
import backtraceio.library.metrics.EventsRequestHandler;
import backtraceio.library.metrics.EventsResult;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.types.BacktraceResultStatus;

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
        context = InstrumentationRegistry.getContext();
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
    public void uploadSummedEventsManual() {
        final Waiter waiter = new Waiter();

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setSummedEventsRequestHandler(mockRequestHandler);

        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.addSummedEvent(summedEventName);
        backtraceClient.metrics.send();

        try {
            waiter.await(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
    }

    @Test
    public void try3TimesOn503AndDropSummedEventsIfMaxNumEventsReached() {
        final Waiter waiter = new Waiter();

        final int timeBetweenRetriesMillis = 20;
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0, timeBetweenRetriesMillis));
        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.statusCode = 503;
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(mockSummedRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.addSummedEvent(summedEventName);
        backtraceClient.metrics.addSummedEvent(summedEventName);

        assertEquals(2, backtraceClient.metrics.getSummedEvents().size());
        backtraceClient.metrics.setMaximumNumberOfEvents(1);
        backtraceClient.metrics.send();

        try {
            waiter.await(1000, 3);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(3, mockSummedRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());
        // We should drop summed event since we failed to send and maximum number of events is too small
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
    }

    @Test
    public void addAndStoreSummedEvent() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.metrics.addSummedEvent(summedEventName));
        assertEquals(1, backtraceClient.metrics.getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.metrics.getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.metrics.getSummedEvents().getFirst().getTimestamp());
    }

    @Test
    public void addSummedEventWithAttributes() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.metrics.addSummedEvent(summedEventName, myCustomAttributes));
        assertEquals(1, backtraceClient.metrics.getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.metrics.getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.metrics.getSummedEvents().getFirst().getTimestamp());
        assertEquals("bar", backtraceClient.metrics.getSummedEvents().getFirst().getAttributes().get("foo"));
    }

    @Test
    public void shouldNotAddNullSummedEvent() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.metrics.addSummedEvent(null));
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
    }

    @Test
    public void shouldNotAddEmptySummedEvent() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.metrics.addSummedEvent(""));
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
    }
}
