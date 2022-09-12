package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
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

@RunWith(AndroidJUnit4.class)
public class BacktraceClientSendTest {
    private Context context;
    private BacktraceCredentials credentials;
    private final String resultMessage = "From request handler";
    private final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("test", "value");
    }};

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    @Test
    public void sendException() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(null, data.report.exception.getMessage(),
                        BacktraceResultStatus.ServerError);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new Exception(this.resultMessage), new OnServerResponseEventListener
                () {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                // THEN
                assertEquals(resultMessage, backtraceResult.message);
                assertEquals(BacktraceResultStatus.ServerError, backtraceResult.status);
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
    public void sendExceptionWithManyCause() {
        // GIVEN
        final String lastExceptionExpectedMessage = "New Exception";
        final Exception causedException = new Exception(new IOException(new IllegalArgumentException(lastExceptionExpectedMessage)));
        final StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = new StackTraceElement("BacktraceClientSendTest.class", "sendCausedException", "BacktraceClientSendTest.java", 1);
        causedException.setStackTrace(stackTraceElements);

        final BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        final String mainExceptionExpectedMessage = "java.io.IOException: java.lang.IllegalArgumentException: New Exception";
        RequestHandler rh = data -> {
            String jsonString = BacktraceSerializeHelper.toJson(data);

            try {
                // THEN
                final JSONObject jsonObject = new JSONObject(jsonString);
                final JSONObject exceptionProperties = jsonObject.getJSONObject("annotations").getJSONObject("Exception properties");
                final String mainExceptionMessage = jsonObject.getJSONObject("annotations").getJSONObject("Exception").getString("message");

                assertEquals(mainExceptionExpectedMessage, mainExceptionMessage);
                assertTrue(exceptionProperties.getJSONArray("stack-trace").length() > 0);
                assertEquals(mainExceptionExpectedMessage, exceptionProperties.get("detail-message"));

                final JSONObject firstCause = exceptionProperties.getJSONObject("cause");
                assertEquals("java.lang.IllegalArgumentException: New Exception", firstCause.getString("detail-message"));

                final JSONObject secondCause = firstCause.getJSONObject("cause");
                assertEquals(lastExceptionExpectedMessage, secondCause.getString("detail-message"));
            } catch (JSONException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            return new BacktraceResult(data.report, data.report.message,
                    BacktraceResultStatus.Ok);
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(causedException), backtraceResult -> {
            waiter.resume();
        });

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceReportWithString() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.message,
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertEquals(resultMessage, backtraceResult.message);
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
    public void sendBacktraceReportWithStringAndAttributes() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
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
    public void sendBacktraceReportWithException() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(new Exception(this
                .resultMessage)), new OnServerResponseEventListener() {
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

    @Test
    public void sendBacktraceReportWithExceptionAndAttributes() {
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
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
    public void sendMultipleReports() {
        // GIVEN
        final Waiter waiter = new Waiter();
        final String[] messages = {"1", "2", "3"};
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);

        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);


        //WHEN
        for (final String message : messages) {
            backtraceClient.send(new Exception(message), new OnServerResponseEventListener() {
                @Override
                public void onEvent(BacktraceResult backtraceResult) {
                    // THEN
                    assertEquals(message, backtraceResult.message);
                    assertEquals(BacktraceResultStatus.Ok, backtraceResult.status);
                    waiter.resume();
                }
            });
        }

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS, 3);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
