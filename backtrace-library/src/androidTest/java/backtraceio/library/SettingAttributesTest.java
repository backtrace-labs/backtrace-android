package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.types.BacktraceResultStatus;


@RunWith(AndroidJUnit4.class)
public class SettingAttributesTest {
    private final String customClientAttributeKey = "custom-client-attribute-id";
    private final String customClientAttributeValue = "custom-client-attribute-value";

    private BacktraceCredentials backtraceCredentials;
    private Map<String, Object> clientAttributes;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        final String url = "https://backtrace.io/";
        backtraceCredentials = new BacktraceCredentials(url);
        clientAttributes = new HashMap<>();

        clientAttributes.put(customClientAttributeKey, customClientAttributeValue);
    }

    @Test
    public void createBacktraceClientWithContextAndCredentials() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(0, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsAndDatabaseSettings() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, new BacktraceDatabaseSettings("/"));
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(0, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsAndDatabase() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase) null);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(0, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsAndAttributes() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, this.clientAttributes);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(this.clientAttributes, client.getAttributes());
        Assert.assertEquals(1, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsDatabaseSettingsAndAttributes() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, new BacktraceDatabaseSettings("/"), this.clientAttributes);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(this.clientAttributes, client.getAttributes());
        Assert.assertEquals(1, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsDatabaseAndAttributes() {
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase) null, this.clientAttributes);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(this.clientAttributes, client.getAttributes());
        Assert.assertEquals(1, size);
    }

    @Test
    public void checkIfAttributesAreInBacktraceData() {
        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceClient backtraceClient = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase) null, this.clientAttributes);
        RequestHandler rh = new TestRequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                assertNotNull(data.attributes);
                assertTrue(data.attributes.containsKey(customClientAttributeKey));
                assertEquals(data.attributes.get(customClientAttributeKey), customClientAttributeValue);
                return new BacktraceResult(data.report, "", BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send("test", new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
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
    public void checkIfAttributesAreSettingWellInUnhandledExceptionHandler() {
//      GIVEN
        final Waiter waiter = new Waiter();
        final String exceptionMessage = "expected!";
        Thread customThread = new Thread(new Runnable() {
            public void run() {
                final BacktraceClient backtraceClient = new BacktraceClient(context, backtraceCredentials);
                RequestHandler rh = new TestRequestHandler() {
                    @Override
                    public BacktraceResult onRequest(BacktraceData data) {
                        waiter.assertTrue(data.report.attributes.containsKey(customClientAttributeKey));
                        waiter.assertEquals(customClientAttributeValue, data.report.attributes.get(customClientAttributeKey));
                        waiter.assertEquals(exceptionMessage, data.report.exception.getMessage());
                        waiter.assertEquals(data.report.attributes.get(BacktraceAttributeConsts.ErrorType), BacktraceAttributeConsts.UnhandledExceptionAttributeType);
                        waiter.resume();
                        return new BacktraceResult(data.report, "", BacktraceResultStatus.Ok);
                    }
                };
                backtraceClient.setOnRequestHandler(rh);
                BacktraceExceptionHandler.enable(backtraceClient);
                BacktraceExceptionHandler.setCustomAttributes(clientAttributes);
                throw new ArithmeticException(exceptionMessage);
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                return;
            }
        });

        // WHEN
        customThread.start();

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS);
            customThread.interrupt();
        } catch (Exception exception) {
            Assert.fail(exception.getMessage());
        }
    }

    @Test
    public void ensureClientCustomAttributesPassedToNewBacktraceAttributes() {
        BacktraceAttributes attributes = new BacktraceAttributes(context, null, clientAttributes);
        assertNotNull(clientAttributes.get("custom-client-attribute-id"));
        assertNotNull(attributes.attributes.get("custom-client-attribute-id"));
        assertEquals(customClientAttributeValue, clientAttributes.get("custom-client-attribute-id"));
        assertEquals(customClientAttributeValue, attributes.attributes.get("custom-client-attribute-id"));
    }
}
