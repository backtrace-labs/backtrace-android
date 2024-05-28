package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import backtraceio.library.common.BacktraceSerializeHelper;
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
public class BacktraceClientMetricsTest {
    public Context context;
    public BacktraceClient backtraceClient;
    public BacktraceCredentials credentials;
    private final String summedEventName = "activity-changed";
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
    public void disableAutoSend() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockRequestHandler);
        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                fail("Should not upload event");
            }
        });

        assertEquals(0, mockRequestHandler.numAttempts);
    }

    @Test
    public void try3TimesOn503() {
        final Waiter waiter = new Waiter();

        final int timeBetweenRetriesMillis = 1;
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0, timeBetweenRetriesMillis));
        final MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        mockUniqueRequestHandler.statusCode = 503;
        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.statusCode = 503;
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(mockUniqueRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });
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

        backtraceClient.metrics.send();

        try {
            waiter.await(5, TimeUnit.SECONDS, 6);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(3, mockUniqueRequestHandler.numAttempts);
        assertFalse(mockUniqueRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(3, mockSummedRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());
    }

    @Test
    public void tryOnceOnHttpError() {
        final Waiter waiter = new Waiter();

        final MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        mockUniqueRequestHandler.statusCode = 404;
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);

        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.statusCode = 404;
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(mockUniqueRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });

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

        // Enabling metrics will automatically send startup events
        final int timeBetweenRetriesMillis = 1;
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0, timeBetweenRetriesMillis));

        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertFalse(mockUniqueRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockSummedRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());

        // We will drop summed events in the case of a non-recoverable server error
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void shouldUploadEventsWhenMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final Waiter waiter = new Waiter();

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
                waiter.resume();
            }
        });

        for (int i = 0; i < maximumNumberOfEvents; i++) {
            backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[i]);
        }

        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(1, mockRequestHandler.numAttempts);
        assertFalse(mockRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(maximumNumberOfEvents, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void shouldNotUploadEventsBeforeMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final Waiter waiter = new Waiter();

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
                waiter.resume();
            }
        });

        // Account for unique events startup event
        for (int i = 0; i < maximumNumberOfEvents - 2; i++) {
            backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]);
        }

        assertEquals(0, mockRequestHandler.numAttempts);
        assertNull(mockRequestHandler.lastEventPayloadJson);
        assertEquals(maximumNumberOfEvents - 1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void uploadEventsAutomatic() {
        final Waiter waiter = new Waiter();

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 1));
        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

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

        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockUniqueRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockSummedRequestHandler.numAttempts);
    }

    @Test
    public void doNotUploadEventsAutomaticBeforeTime() {
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 100));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
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

        backtraceClient.metrics.addUniqueEvent(uniqueAttributeName[0]);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());

        assertNull(mockRequestHandler.lastEventPayloadJson);
        assertEquals(0, mockRequestHandler.numAttempts);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.metrics.getUniqueEvents().size());
        //assertEquals(1, backtraceClient.metrics.getSummedEvents().size());
    }

    @Test
    public void shouldAllowToOverrideUniqueEventName() {
        final Waiter waiter = new Waiter();
        final String uniqueEventAttributeName = "uniqueEventAttributeName";
        final String uniqueEventAttributeValue = "SomeRandomText123123";

        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);
        backtraceClient.attributes.put(uniqueEventAttributeName, uniqueEventAttributeValue);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJsonString = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJsonString.length());

                JSONObject json;
                try {
                    json = new JSONObject(eventsPayloadJsonString);
                    JSONObject uniqueEventJson = json.getJSONArray("unique_events").getJSONObject(0);
                    assertEquals(uniqueEventAttributeName, uniqueEventJson.getJSONArray("unique").get(0));
                    assertNotNull(uniqueEventJson.getJSONObject("attributes").getString("guid"));
                } catch (Exception e) {
                    fail(e.toString());
                }

                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                waiter.resume();
            }
        });
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0), uniqueEventAttributeName);

        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockUniqueRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
    }

    @Test
    public void sendStartupEvent() {
        final Waiter waiter = new Waiter();

        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJsonString = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJsonString.length());

                JSONObject json;
                try {
                    json = new JSONObject(eventsPayloadJsonString);
                    assertEquals("guid", json.getJSONArray("unique_events").getJSONObject(0).getJSONArray("unique").get(0));
                    assertNotNull(json.getJSONArray("unique_events").getJSONObject(0).getJSONObject("attributes").getString("guid"));
                } catch (Exception e) {
                    fail(e.toString());
                }

                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });
        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJsonString = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJsonString.length());

                JSONObject json;
                try {
                    json = new JSONObject(eventsPayloadJsonString);
                    assertEquals("Application Launches", json.getJSONArray("summed_events").getJSONObject(0).getString("metric_group"));
                } catch (Exception e) {
                    fail(e.toString());
                }

                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));


        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertFalse(mockUniqueRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertFalse(mockSummedRequestHandler.lastEventPayloadJson.isEmpty());
        assertEquals(1, mockSummedRequestHandler.numAttempts);

        assertEquals(1, backtraceClient.metrics.getUniqueEvents().size());
        assertEquals(0, backtraceClient.metrics.getSummedEvents().size());
    }


    @Test
    public void metricsAttributesShouldChangeIfClientAttributeChanges() {
        final Waiter waiter = new Waiter();

        backtraceClient.attributes.put("foo", "bar");
        backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials, defaultBaseUrl, 0));
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setUniqueEventsRequestHandler(mockRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.metrics.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(BacktraceResultStatus.Ok, result.status);
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertTrue(eventsPayloadJson.contains("\"foo\":\"bar\""));
                assertFalse(eventsPayloadJson.contains("\"foo\":\"baz\""));
                waiter.resume();
            }
        });
        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(BacktraceResultStatus.Ok, result.status);
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertTrue(eventsPayloadJson.contains("\"foo\":\"bar\""));
                assertFalse(eventsPayloadJson.contains("\"foo\":\"baz\""));
                waiter.resume();
            }
        });

        backtraceClient.metrics.addSummedEvent(summedEventName);
        backtraceClient.metrics.send();

        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        backtraceClient.attributes.put("foo", "baz");

        backtraceClient.metrics.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(BacktraceResultStatus.Ok, result.status);
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertFalse(eventsPayloadJson.contains("\"foo\":\"bar\""));
                assertTrue(eventsPayloadJson.contains("\"foo\":\"baz\""));
                waiter.resume();
            }
        });
        backtraceClient.metrics.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(BacktraceResultStatus.Ok, result.status);
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertFalse(eventsPayloadJson.isEmpty());
                assertFalse(eventsPayloadJson.contains("\"foo\":\"bar\""));
                assertTrue(eventsPayloadJson.contains("\"foo\":\"baz\""));
                waiter.resume();
            }
        });

        backtraceClient.metrics.addSummedEvent(summedEventName);
        backtraceClient.metrics.send();

        try {
            waiter.await(5, TimeUnit.SECONDS, 2);
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
