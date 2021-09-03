package backtraceio.library;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.metrics.BacktraceMetrics;
import backtraceio.library.metrics.Event;
import backtraceio.library.metrics.EventsOnServerResponseEventListener;
import backtraceio.library.metrics.EventsPayload;
import backtraceio.library.metrics.EventsRequestHandler;
import backtraceio.library.metrics.EventsResult;
import backtraceio.library.metrics.UniqueEvent;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.types.BacktraceResultStatus;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientMetricsTest {
    public Context context;
    public BacktraceClient backtraceClient;
    public BacktraceCredentials credentials;
    private final String summedEventName = "activity-changed";
    // existing attribute name in Backtrace
    private final String uniqueAttributeName[] = {"uname.version", "cpu.boottime", "screen.orientation", "battery.state", "device.airplane_mode", "device.sdk", "device.brand", "system.memory.total", "uname.sysname", "application.package"};

    private final String defaultBaseUrl = BacktraceMetrics.defaultBaseUrl;
    private final String token = "aaaaabbbbbccccf82668682e69f59b38e0a853bed941e08e85f4bf5eb2c5458";
    private final String universeName = "testing-universe-name";
    private final String _uniqueEventsSubmissionUrl = "unique-events/submit?token=" + token + "&universe=" + universeName;
    private final String _summedEventsSubmissionUrl = "summed-events/submit?token=" + token + "&universe=" + universeName;

    static {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
        BacktraceDatabase database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());

        backtraceClient = new BacktraceClient(context, credentials, database);

        BacktraceLogger.setLevel(LogLevel.DEBUG);
    }

    @After
    public void cleanUp() {

    }

    //@Test
    //public void testUploadSummedEventsOnly()

    public class MockRequestHandler implements EventsRequestHandler {
        public int numAttempts = 0;
        public int errorCode = 200;
        public String lastEventPayloadJson;

        @Override
        public EventsResult onRequest(EventsPayload data) {
            String eventsPayloadJson = BacktraceSerializeHelper.toJson(data);
            lastEventPayloadJson = eventsPayloadJson;
            numAttempts++;

            BacktraceResultStatus status;
            if (errorCode == 200) {
                status = BacktraceResultStatus.Ok;
            } else {
                status = BacktraceResultStatus.ServerError;
            }
            return new EventsResult(data, eventsPayloadJson, status, errorCode);
        }
    }

    @Test
    public void disableAutoSend() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        try {
            sleep(100);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals(0, mockRequestHandler.numAttempts);
    }

    @Test
    public void uploadSummedEventsManual() {
        final Waiter waiter = new Waiter();

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setSummedEventsRequestHandler(mockRequestHandler);

        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.addSummedEvent(summedEventName);

        assertEquals(1, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(1, backtraceClient.getUniqueEvents().size());

        backtraceClient.sendMetrics();

        try {
            waiter.await(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertNotEquals(0, mockRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockRequestHandler.numAttempts);
        assertEquals(0, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(1, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void uploadUniqueEventsManual() {
        final Waiter waiter = new Waiter();

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.sendMetrics();

        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        try {
            waiter.await(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertNotEquals(0, mockRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockRequestHandler.numAttempts);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        assertEquals(0, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void doNotUploadWhenNoEventsAvailable() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        // Clear startup event
        ConcurrentLinkedDeque<UniqueEvent> uniqueEvents = backtraceClient.getUniqueEvents();
        uniqueEvents.clear();

        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        // When no events in queue request handler should not be called
        backtraceClient.sendMetrics();
        try {
            sleep(100);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals(0, mockRequestHandler.numAttempts);

        // When at least one event is in queue request handler should be called
        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.sendMetrics();
        try {
            sleep(100);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals(1, mockRequestHandler.numAttempts);
    }

    @Test
    public void try3TimesOn503() {
        final Waiter waiter = new Waiter();

        final int timeBetweenRetriesMillis = 10;
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0, timeBetweenRetriesMillis);
        final MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        mockUniqueRequestHandler.errorCode = 503;
        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.errorCode = 503;
        backtraceClient.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        backtraceClient.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockUniqueRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });
        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockSummedRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.addSummedEvent(summedEventName);

        assertEquals(1, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        backtraceClient.sendMetrics();

        try {
            waiter.await(1000, 6);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(3, mockUniqueRequestHandler.numAttempts);
        assertNotEquals(0, mockUniqueRequestHandler.lastEventPayloadJson.length());
        assertEquals(3, mockSummedRequestHandler.numAttempts);
        assertNotEquals(0, mockSummedRequestHandler.lastEventPayloadJson.length());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        // We should keep summed event since we failed to send
        assertEquals(1, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void try3TimesOn503AndDropSummedEventsIfMaxNumEventsReached() {
        final Waiter waiter = new Waiter();

        final int timeBetweenRetriesMillis = 10;
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0, timeBetweenRetriesMillis);
        final MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        mockUniqueRequestHandler.errorCode = 503;
        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.errorCode = 503;
        backtraceClient.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        backtraceClient.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockUniqueRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });
        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockSummedRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
                waiter.resume();
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.addSummedEvent(summedEventName);
        backtraceClient.addSummedEvent(summedEventName);

        assertEquals(2, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        backtraceClient.setMaximumNumberOfEvents(1);
        backtraceClient.sendMetrics();

        try {
            waiter.await(1000, 6);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(3, mockUniqueRequestHandler.numAttempts);
        assertNotEquals(0, mockUniqueRequestHandler.lastEventPayloadJson.length());
        assertEquals(3, mockSummedRequestHandler.numAttempts);
        assertNotEquals(0, mockSummedRequestHandler.lastEventPayloadJson.length());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        // We should drop summed event since we failed to send and maximum number of events is too small
        assertEquals(0, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void tryOnceOnHttpError() {
        final int timeBetweenRetriesMillis = 10;
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0, timeBetweenRetriesMillis);

        final MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        mockUniqueRequestHandler.errorCode = 404;
        backtraceClient.setUniqueEventsRequestHandler(mockUniqueRequestHandler);

        final MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        mockSummedRequestHandler.errorCode = 404;
        backtraceClient.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockUniqueRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
            }
        });

        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(mockSummedRequestHandler.numAttempts - 1, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.ServerError, result.status);
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.addSummedEvent(summedEventName);
        assertEquals(1, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        backtraceClient.sendMetrics();

        for (int i = 0; i < BacktraceMetrics.maxNumberOfAttempts; i++) {
            try {
                sleep(timeBetweenRetriesMillis * (long) Math.pow(10, i));
            } catch (Exception e) {
                fail(e.toString());
            }
            assertEquals(1, mockUniqueRequestHandler.numAttempts);
        }

        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertNotEquals(0, mockUniqueRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockSummedRequestHandler.numAttempts);
        assertNotEquals(0, mockSummedRequestHandler.lastEventPayloadJson.length());

        // We will drop summed events in the case of a non-recoverable server error
        assertEquals(0, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void doNotAddMoreUniqueEventsWhenMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final int numberOfTestEventsToAdd = 10;

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        backtraceClient.setMaximumNumberOfEvents(maximumNumberOfEvents);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
            }
        });

        // All unique attributes must have different unique attribute names
        for (int i = 0; i < numberOfTestEventsToAdd; i++) {
            backtraceClient.addUniqueEvent(uniqueAttributeName[i]);
        }

        assertEquals(maximumNumberOfEvents, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void shouldUploadEventsWhenMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final Waiter waiter = new Waiter();

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        backtraceClient.setMaximumNumberOfEvents(maximumNumberOfEvents);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        for (int i = 0; i < maximumNumberOfEvents; i++) {
            backtraceClient.addUniqueEvent(uniqueAttributeName[i]);
        }

        try {
            waiter.await(1000);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(1, mockRequestHandler.numAttempts);
        assertNotEquals(0, mockRequestHandler.lastEventPayloadJson.length());
        assertEquals(maximumNumberOfEvents, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void shouldNotUploadEventsBeforeMaxNumEventsReached() {
        final int maximumNumberOfEvents = 3;
        final Waiter waiter = new Waiter();

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);
        backtraceClient.setMaximumNumberOfEvents(maximumNumberOfEvents);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        // Account for unique events startup event
        for (int i = 0; i < maximumNumberOfEvents - 2; i++) {
            backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        }

        try {
            sleep(100);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(0, mockRequestHandler.numAttempts);
        assertNull(mockRequestHandler.lastEventPayloadJson);
        assertEquals(maximumNumberOfEvents - 1, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void uploadEventsAutomatic() {
        final Waiter waiter = new Waiter();

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 200);
        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });
        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        backtraceClient.addSummedEvent(summedEventName);
        assertEquals(1, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        try {
            waiter.await(1000, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertNotEquals(0, mockUniqueRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertNotEquals(0, mockSummedRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockSummedRequestHandler.numAttempts);
        assertEquals(0, backtraceClient.getSummedEvents().size());
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void doNotUploadEventsAutomaticBeforeTime() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 100);
        MockRequestHandler mockRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                //Assert.assertEquals(resultMessage, result.message);
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJson = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJson.length());
                assertEquals(BacktraceResultStatus.Ok, result.status);
            }
        });

        backtraceClient.addUniqueEvent(uniqueAttributeName[0]);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        try {
            sleep(50);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertNull(mockRequestHandler.lastEventPayloadJson);
        assertEquals(0, mockRequestHandler.numAttempts);
        // We will always have startup unique event GUID
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        //assertEquals(1, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void sendStartupEvent() {
        final Waiter waiter = new Waiter();

        MockRequestHandler mockUniqueRequestHandler = new MockRequestHandler();
        backtraceClient.setUniqueEventsRequestHandler(mockUniqueRequestHandler);
        MockRequestHandler mockSummedRequestHandler = new MockRequestHandler();
        backtraceClient.setSummedEventsRequestHandler(mockSummedRequestHandler);

        backtraceClient.setUniqueEventsOnServerResponse(new EventsOnServerResponseEventListener() {
            @Override
            public void onEvent(EventsResult result) {
                assertEquals(0, result.getEventsPayload().getDroppedEvents());
                String eventsPayloadJsonString = BacktraceSerializeHelper.toJson(result.getEventsPayload());
                assertNotEquals(0, eventsPayloadJsonString.length());

                JSONObject json;
                try {
                    json = new JSONObject(eventsPayloadJsonString);
                    assertEquals("guid", json.getJSONArray("unique_events").getJSONObject(0).getString("unique"));
                    assertNotNull(json.getJSONArray("unique_events").getJSONObject(0).getJSONObject("attributes").getString("guid"));
                } catch (Exception e) {
                    fail(e.toString());
                }

                assertEquals(BacktraceResultStatus.Ok, result.status);
                waiter.resume();
            }
        });
        backtraceClient.setSummedEventsOnServerResponse(new EventsOnServerResponseEventListener() {
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

        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        try {
            waiter.await(1000, 2);
        } catch (Exception e) {
            fail(e.toString());
        }

        assertNotEquals(0, mockUniqueRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockUniqueRequestHandler.numAttempts);
        assertNotEquals(0, mockSummedRequestHandler.lastEventPayloadJson.length());
        assertEquals(1, mockSummedRequestHandler.numAttempts);

        assertEquals(1, backtraceClient.getUniqueEvents().size());
        assertEquals(0, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void addAndStoreSummedEvent() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertTrue(backtraceClient.addSummedEvent(summedEventName));
        assertEquals(1, backtraceClient.getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.getSummedEvents().getFirst().getTimestamp());
    }

    @Test
    public void addSummedEventWithAttributes() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.addSummedEvent(summedEventName, myCustomAttributes));
        assertEquals(1, backtraceClient.getSummedEvents().size());

        assertEquals(summedEventName, backtraceClient.getSummedEvents().getFirst().getName());
        assertNotEquals(0, backtraceClient.getSummedEvents().getFirst().getTimestamp());
        assertEquals("bar", backtraceClient.getSummedEvents().getFirst().getAttributes().get("foo"));
    }

    @Test
    public void shouldNotAddNullSummedEvent() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertFalse(backtraceClient.addSummedEvent(null));
        assertEquals(0, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void shouldNotAddEmptySummedEvent() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertFalse(backtraceClient.addSummedEvent(""));
        assertEquals(0, backtraceClient.getSummedEvents().size());
    }

    @Test
    public void addAndStoreUniqueEvent() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllBacktraceAttributes());

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        expectedAttributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventNullAttributes() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0], null));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getUniqueEvents().getLast().getTimestamp());

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllBacktraceAttributes());

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        expectedAttributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        assertEquals(expectedAttributes.size(), backtraceClient.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void addAndStoreUniqueEventWithAttributes() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0], myCustomAttributes));
        // Account for the startup unique event
        assertEquals(2, backtraceClient.getUniqueEvents().size());

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getUniqueEvents().getLast().getTimestamp());
        assertEquals("bar", backtraceClient.getUniqueEvents().getLast().getAttributes().get("foo"));

        // See how we get all different kinds of attributes in backtraceio.library.models.BacktraceData.setAttributes
        Map<String, Object> expectedAttributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, backtraceClient.attributes);
        expectedAttributes.putAll(backtraceAttributes.getAllBacktraceAttributes());

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        expectedAttributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        assertEquals(expectedAttributes.size() + 1, backtraceClient.getUniqueEvents().getLast().getAttributes().size());
    }

    @Test
    public void doNotAddUniqueEventIfUniqueAttributeNotDefined() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertFalse(backtraceClient.addUniqueEvent("undefined-attribute"));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void doAddUniqueEventIfUniqueAttributeDefinedInCustomAttributes() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        final String expectedKey = "foo";
        final String expectedValue = "bar";
        Map<String, Object> myCustomAttributes = new HashMap<String, Object>() {{
            put(expectedKey, expectedValue);
        }};
        assertTrue(backtraceClient.addUniqueEvent(expectedKey, myCustomAttributes));

        // Account for the startup unique event
        assertEquals(2, backtraceClient.getUniqueEvents().size());
        assertEquals(expectedKey, backtraceClient.getUniqueEvents().getLast().getName());
        assertNotEquals(0, backtraceClient.getUniqueEvents().getLast().getTimestamp());
        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void doNotAddNullUniqueEvent() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertFalse(backtraceClient.addUniqueEvent(null));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void doNotAddUniqueEventEmptyString() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertFalse(backtraceClient.addUniqueEvent(""));
        // Account for the startup unique event
        assertEquals(1, backtraceClient.getUniqueEvents().size());
    }

    @Test
    public void uniqueAttributesPerEventDoNotMutate() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        String expectedKey = "foo";
        String expectedValue1 = "bar";
        String expectedValue2 = "baz";
        backtraceClient.attributes.put(expectedKey, expectedValue1);
        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));

        backtraceClient.attributes.put(expectedKey, expectedValue2);
        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[1]));

        assertEquals(3, backtraceClient.getUniqueEvents().size());
        Event event2 = backtraceClient.getUniqueEvents().getLast();
        backtraceClient.getUniqueEvents().removeLast();
        Event event1 = backtraceClient.getUniqueEvents().getLast();

        assertEquals(expectedValue1, event1.getAttributes().get(expectedKey));
        assertEquals(expectedValue2, event2.getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventWithCustomAttributeExistsEvenIfCustomAttributeDeletedLater() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        String expectedKey = "foo";
        String expectedValue = "bar";
        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.addUniqueEvent(expectedKey));

        backtraceClient.attributes.remove(expectedKey);

        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));
        assertEquals(expectedKey, backtraceClient.getUniqueEvents().getLast().getName());
    }

    @Test
    public void uniqueEventUpdateTimestamp() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        long previousTimestamp = backtraceClient.getUniqueEvents().getLast().getTimestamp();

        try {
            sleep(1000);
        } catch (Exception e){
            fail(e.toString());
        }
        // Force update
        backtraceClient.sendMetrics();

        long updatedTimestamp = backtraceClient.getUniqueEvents().getLast().getTimestamp();

        assertTrue(updatedTimestamp > previousTimestamp);
    }

    @Test
    public void uniqueEventUpdateAttributes() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        String expectedKey = "foo";
        String expectedValue = "bar";

        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertNull(backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, expectedValue);
        // It should not be added to the unique event yet
        assertNull(backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        // Force update
        backtraceClient.sendMetrics();

        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventEmptyAttributeValueShouldNotOverridePreviousValueOnUpdate() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        String expectedKey = "foo";
        String expectedValue = "bar";

        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, "");
        assertEquals("", backtraceClient.attributes.get(expectedKey));

        // Force update
        backtraceClient.sendMetrics();

        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }

    @Test
    public void uniqueEventNullAttributeValueShouldNotOverridePreviousValueOnUpdate() {
        backtraceClient.enableMetrics(defaultBaseUrl, universeName, token, 0);

        String expectedKey = "foo";
        String expectedValue = "bar";

        backtraceClient.attributes.put(expectedKey, expectedValue);
        assertTrue(backtraceClient.addUniqueEvent(uniqueAttributeName[0]));

        assertEquals(uniqueAttributeName[0], backtraceClient.getUniqueEvents().getLast().getName());
        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));

        backtraceClient.attributes.put(expectedKey, null);
        assertNull(backtraceClient.attributes.get(expectedKey));

        // Force update
        backtraceClient.sendMetrics();

        assertEquals(expectedValue, backtraceClient.getUniqueEvents().getLast().getAttributes().get(expectedKey));
    }
}
