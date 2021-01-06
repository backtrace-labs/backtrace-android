package backtraceio.library;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientProguardTest {
    private Context context;
    private BacktraceCredentials credentials;
    private final String resultMessage = "From request handler";
    private final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("test", "value");
    }};
    // For Proguard
    private final UUID proguardMappingUUID = UUID.randomUUID();
    private final Map<String, Object> proguardAttributes = new HashMap<String, Object>() {{
        put("symbolication_id", proguardMappingUUID.toString());
    }};

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    @Test
    public void sendBacktraceReportWithStringAndAttributesProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        backtraceClient.enableProguard();

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertEquals("proguard", data.symbolication);
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage, this.proguardAttributes),
                new OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNull(backtraceResult.getBacktraceReport().exception);
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
    public void sendBacktraceReportWithStringAndAttributesNoProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertNull(data.symbolication);
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage, this.attributes),
                new OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(attributes.get("test"),
                                backtraceResult.getBacktraceReport().attributes.get("test")
                        );
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNull(backtraceResult.getBacktraceReport().exception);
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
    public void sendBacktraceReportWithExceptionAndAttributesProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        backtraceClient.enableProguard();

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertEquals("proguard", data.symbolication);
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(
                new Exception(this.resultMessage), this.proguardAttributes), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceReportWithExceptionAndAttributesNoProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertNull(data.symbolication);
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(
                new Exception(this.resultMessage), this.attributes), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertEquals(attributes.get("test"),
                                backtraceResult.getBacktraceReport().attributes.get("test")
                        );
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceExceptionProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, proguardAttributes);
        backtraceClient.enableProguard();

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertEquals("proguard", data.symbolication);
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception(resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceExceptionNoProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, proguardAttributes);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertNull(data.symbolication);
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception(resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNotNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceStringProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, proguardAttributes);
        backtraceClient.enableProguard();

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertEquals("proguard", data.symbolication);
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(resultMessage, new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceStringNoProguard() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, proguardAttributes);

        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertNull(data.symbolication);
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(resultMessage, new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {

                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
                        assertEquals(proguardAttributes.get("symbolication_id"),
                                backtraceResult.getBacktraceReport().attributes.get("symbolication_id")
                        );
                        assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                        assertNotNull(backtraceResult.getBacktraceReport());
                        assertNull(backtraceResult.getBacktraceReport().exception);
                        waiter.resume();
                    }
                }
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
