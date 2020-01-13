package backtraceio.library;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.types.BacktraceResultStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class SettingAttributesTest {
    private final String customClientAttributeKey = "custom-client-attribute-id";
    private final String customClientAttributeValue = "custom-client-attribute-value";

    private BacktraceCredentials backtraceCredentials;
    private Map<String, Object> clientAttributes;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        final String url = "https://backtrace.io/";
        backtraceCredentials = new BacktraceCredentials(url);
        clientAttributes = new HashMap<>();

        clientAttributes.put(customClientAttributeKey, customClientAttributeValue);

    }

    @Test
    public void createBacktraceClientWithContextAndCredentials(){
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
    public void createBacktraceClientWithContextCredentialsAndDatabaseSettings(){
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
    public void createBacktraceClientWithContextCredentialsAndDatabase(){
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase)null);
        // WHEN
        Map<String, Object> attributes = client.getAttributes();
        int size = attributes.size();

        // THEN
        Assert.assertNotNull(attributes);
        Assert.assertEquals(0, size);
    }

    @Test
    public void createBacktraceClientWithContextCredentialsAndAttributes(){
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
    public void createBacktraceClientWithContextCredentialsDatabaseSettingsAndAttributes(){
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
    public void createBacktraceClientWithContextCredentialsDatabaseAndAttributes(){
        // GIVEN
        BacktraceClient client = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase)null, this.clientAttributes);
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
        BacktraceClient backtraceClient = new BacktraceClient(context, this.backtraceCredentials, (BacktraceDatabase)null, this.clientAttributes);
        RequestHandler rh = new RequestHandler() {
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
}
