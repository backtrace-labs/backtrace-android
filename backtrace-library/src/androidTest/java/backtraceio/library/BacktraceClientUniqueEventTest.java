package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static java.lang.Thread.sleep;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.metrics.Event;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.EventsResult;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceMetrics;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientUniqueEventTest {
    public Context context;
    public BacktraceClient backtraceClient;
    public BacktraceCredentials credentials;
    // existing attribute name in Backtrace
    private final String[] uniqueAttributeName = {"uname.version", "cpu.boottime", "screen.orientation", "battery.state", "device.airplane_mode", "device.sdk", "device.brand", "system.memory.total", "uname.sysname", "application.package"};

    private final String defaultBaseUrl = BacktraceMetrics.defaultBaseUrl;
    private final String token = "aaaaabbbbbccccf82668682e69f59b38e0a853bed941e08e85f4bf5eb2c5458";

    /**
     * NOTE: Some of these tests are very time-sensitive so you may occasionally get false negative results.
     * For best results run under low CPU load and low memory utilization conditions.
     */

    @Before
    public void setUp() {
        BacktraceLogger.setLogger(new BacktraceInternalLogger(LogLevel.DEBUG));
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://universe.sp.backtrace.io:6098", token);
        BacktraceDatabase database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());

        backtraceClient = new BacktraceClient(context, credentials, database);
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
    public void uploadDefaultUniqueEventManual() {
        final Waiter waiter = new Waiter();

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.send();

        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void uploadUniqueEventsManual() {
        final Waiter waiter = new Waiter();

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.metrics.send();

        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void doNotUploadWhenNoEventsAvailable() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        // Clear startup event
        ConcurrentLinkedDeque<UniqueEvent> uniqueEvents = backtraceClient.metrics.getUniqueEvents();
        uniqueEvents.clear();

        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                fail("Should not upload event");
            }
        });

        // When no events in queue request handler should not be called
        backtraceClient.metrics.send();
        assertEquals(0, mockUniqueRequestHandler.numAttempts);
    }

    @Test
    public void doNotAddMoreUniqueEventsWhenMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final int numberOfTestEventsToAdd = 10;

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        backtraceClient.metrics.setMaximumNumberOfEvents(maximumNumberOfEvents);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
            }
        });

        // All unique attributes must have different unique attribute names
        for (int i = 0; i < numberOfTestEventsToAdd; i++) {
            backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[i]);
        }

        assertEquals(maximumNumberOfEvents, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void addAndStoreUniqueEvent() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.metrics.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventNullAttributes() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0], null));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.metrics.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventWithAttributes() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0], myCustomAttributes));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.metrics.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp());
        assertEquals("bar", backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get("foo"));

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size() + 1, backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void doNotAddUniqueEventIfUniqueAttributeNotDefined() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.metrics.addUniqueEvent("undefined-attribute"));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void doAddUniqueEventIfUniqueAttributeDefinedInCustomAttributes() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        final String expectedKey = "foo";
        final String expectedValue = "bar";
        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put(expectedKey, expectedValue);
        }};
        assertTrue(backtraceClient.metrics.addUniqueEvent(expectedKey, myCustomAttributes));

        // Account for the startup unique event
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());
        assertEquals(expectedKey, backtraceClient.metrics.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp());
        assertEquals(expectedValue, backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void doNotAddNullUniqueEvent() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.metrics.addUniqueEvent(null));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void doNotAddUniqueEventEmptyString() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.metrics.addUniqueEvent(""));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void uniqueAttributesPerEventDoNotMutate() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue1 = "bar";
        String expectedValue2 = "baz";
        backtraceClient.attributes.put(expectedKey, expectedValue1);
        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]));

        backtraceClient.attributes.put(expectedKey, expectedValue2);
        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[1]));

        assertEquals(3, backtraceClient.metrics.getUniqueEvents().size());
        Event event2 = backtraceClient.metrics.getUniqueEvents().getLast();
        backtraceClient.metrics.getUniqueEvents().removeLast();
        Event event1 = backtraceClient.metrics.getUniqueEvents().getLast();

        assertEquals(expectedValue1, event1.getAttributes().get(expectedKey));
        assertEquals(expectedValue2, event2.getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventWithCustomAttributeExistsEvenIfCustomAttributeDeletedLater() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";
        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.metrics.addUniqueEvent(expectedKey));

        backtraceClient.attributes.remove(expectedKey);

        assertEquals(expectedValue, backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get(expectedKey));
        assertEquals(expectedKey, backtraceClient.metrics.getUniqueEvents().getLast().getName());
    }

    @Test
    public void uniqueEventUpdateTimestamp() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.metrics.getUniqueEvents().getLast().getName());
        long previousTimestamp = backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp();

        // Wait 1 second so that the timestamp will update on the next send.
        // Timestamp granularity is 1 second
        try {
            sleep(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        // Force update
        backtraceClient.metrics.send();

        long updatedTimestamp = backtraceClient.metrics.getUniqueEvents().getLast().getTimestamp();

        assertTrue(updatedTimestamp > previousTimestamp);
    }

    @Test
    public void uniqueEventUpdateAttributes() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";

        assertTrue(backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.metrics.getUniqueEvents().getLast().getName());
        assertNull(backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, expectedValue);
        // It should not be added to the unique event yet
        assertNull(backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        // Force update
        backtraceClient.metrics.send();

        assertEquals(expectedValue, backtraceClient.metrics.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }
}
