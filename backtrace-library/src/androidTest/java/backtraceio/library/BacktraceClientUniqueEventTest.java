package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static java.lang.Thread.sleep;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.UnsupportedMetricsServer;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.metrics.Event;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.EventsResult;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.models.metrics.Event;
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
    public void uploadDefaultUniqueEventManual() throws UnsupportedMetricsServer {
        final Waiter waiter = new Waiter();

        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.getMetrics().setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.getMetrics().send();

        assertEquals(1, backtraceClient.getMetrics().getUniqueEvents().size());

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        assertEquals(1, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void uploadUniqueEventsManual() throws UnsupportedMetricsServer {
        final Waiter waiter = new Waiter();

        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.getMetrics().setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.getMetrics().send();

        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockRequestHandler.numAttempts);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void doNotUploadWhenNoEventsAvailable() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        // Clear startup event
        ConcurrentLinkedDeque<UniqueEvent> uniqueEvents = backtraceClient.getMetrics().getUniqueEvents();
        uniqueEvents.clear();

        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setUniqueEventsRequestHandler(mockUniqueRequestHandler);

        backtraceClient.getMetrics().setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                fail("Should not upload event");
            }
        });

        // When no events in queue request handler should not be called
        backtraceClient.getMetrics().send();
        assertEquals(0, mockUniqueRequestHandler.numAttempts);
    }

    @Test
    public void doNotAddMoreUniqueEventsWhenMaxNumEventsReached() throws UnsupportedMetricsServer {
        final int maximumNumberOfEvents = 3;
        final int numberOfTestEventsToAdd = 10;

        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        backtraceClient.getMetrics().setMaximumNumberOfEvents(maximumNumberOfEvents);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.getMetrics().setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.getMetrics().setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
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
            backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[i]);
        }

        assertEquals(maximumNumberOfEvents, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void addAndStoreUniqueEvent() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventNullAttributes() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0], null));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventWithAttributes() throws UnsupportedMetricsServer{
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0], myCustomAttributes));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp());
        assertEquals("bar", backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get("foo"));

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllAttributes());

        assertEquals(expectedAttributes.size() + 1, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void doNotAddUniqueEventIfUniqueAttributeNotDefined() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.getMetrics().addUniqueEvent("undefined-attribute"));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void doAddUniqueEventIfUniqueAttributeDefinedInCustomAttributes() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        final String expectedKey = "foo";
        final String expectedValue = "bar";
        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put(expectedKey, expectedValue);
        }};
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(expectedKey, myCustomAttributes));

        // Account for the startup unique event
        assertEquals(2, backtraceClient.getMetrics().getUniqueEvents().size());
        assertEquals(expectedKey, backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp());
        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void doNotAddNullUniqueEvent() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.getMetrics().addUniqueEvent(null));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void doNotAddUniqueEventEmptyString() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertFalse(backtraceClient.getMetrics().addUniqueEvent(""));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getMetrics().getUniqueEvents().size());
    }

    @Test
    public void uniqueAttributesPerEventDoNotMutate() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue1 = "bar";
        String expectedValue2 = "baz";
        backtraceClient.attributes.put(expectedKey, expectedValue1);
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));

        backtraceClient.attributes.put(expectedKey, expectedValue2);
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[1]));

        assertEquals(3, backtraceClient.getMetrics().getUniqueEvents().size());
        Event event2 = backtraceClient.getMetrics().getUniqueEvents().getLast();
        backtraceClient.getMetrics().getUniqueEvents().removeLast();
        Event event1 = backtraceClient.getMetrics().getUniqueEvents().getLast();

        assertEquals(expectedValue1, event1.getAttributes().get(expectedKey));
        assertEquals(expectedValue2, event2.getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventWithCustomAttributeExistsEvenIfCustomAttributeDeletedLater() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";
        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(expectedKey));

        backtraceClient.attributes.remove(expectedKey);

        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));
        assertEquals(expectedKey, backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
    }

    @Test
    public void uniqueEventUpdateTimestamp() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        long previousTimestamp = backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp();

        // Wait 1 second so that the timestamp will update on the next send.
        // Timestamp granularity is 1 second
        try {
            sleep(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        // Force update
        backtraceClient.getMetrics().send();

        long updatedTimestamp = backtraceClient.getMetrics().getUniqueEvents().getLast().getTimestamp();

        assertTrue(updatedTimestamp > previousTimestamp);
    }

    @Test
    public void uniqueEventUpdateAttributes() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";

        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertNull(backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, expectedValue);
        // It should not be added to the unique event yet
        assertNull(backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));

        // Force update
        backtraceClient.getMetrics().send();

        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventEmptyAttributeValueShouldNotOverridePreviousValueOnUpdate() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";

        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, "");
        assertEquals("", backtraceClient.attributes.get(expectedKey));

        // Force update
        backtraceClient.getMetrics().send();

        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventNullAttributeValueShouldNotOverridePreviousValueOnUpdate() throws UnsupportedMetricsServer {
        backtraceClient.getMetrics().enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));

        String expectedKey = "foo";
        String expectedValue = "bar";

        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.getMetrics().addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getMetrics().getUniqueEvents().getLast().getName());
        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, null);
        assertNull(backtraceClient.attributes.get(expectedKey));

        // Force update
        backtraceClient.getMetrics().send();

        assertEquals(expectedValue, backtraceClient.getMetrics().getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }
}
