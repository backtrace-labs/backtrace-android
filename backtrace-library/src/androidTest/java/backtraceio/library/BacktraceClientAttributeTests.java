package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientAttributeTests {
    private Context context;
    private BacktraceCredentials credentials;
    private BacktraceDatabase database;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
        database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());
    }

    @Test
    public void shouldAddASingleAttribute() {
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        backtraceClient.addAttribute(attributeKey, attributeValue);

        Map<String, Object> attributes = backtraceClient.getAttributes();

        Object value = attributes.get(attributeKey);
        assertNotNull(value);
        assertEquals(value, attributeValue);
    }

    @Test
    public void shouldAddMultipleAttributesAtOnce() {
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";
        final Integer numberOfAttributesToVerify = 3;
        Map<String, Object> attributes = new HashMap<>();
        for (int attributeIteration = 0; attributeIteration < numberOfAttributesToVerify; attributeIteration++) {
               attributes.put(String.format("%s %d", attributeKey, attributeIteration), attributeValue);
        }
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        backtraceClient.addAttribute(attributes);

        Map<String, Object> clientAttributes = backtraceClient.getAttributes();

        for (int attributeIteration = 0; attributeIteration < numberOfAttributesToVerify; attributeIteration++) {
            Object value = attributes.get(String.format("%s %d", attributeKey, attributeIteration));
            assertNotNull(value);
            assertEquals(value, attributeValue);
        }
    }

    @Test
    public void shouldReplaceExistingAttribute() {
        final String attributeKey = "test-attribute";
        final String oldAttributeValue = "old-test-value";
        final String newAttributeValue = "test-value-new";

        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        backtraceClient.addAttribute(attributeKey, oldAttributeValue);

        backtraceClient.addAttribute(attributeKey, newAttributeValue);

        Map<String, Object> attributes = backtraceClient.getAttributes();

        Object value = attributes.get(attributeKey);
        assertNotNull(value);
        assertEquals(value, newAttributeValue);
    }

    @Test
    public void attributesShouldBeAvailableInReport() {
        final String errorMessage = "error message";
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        backtraceClient.addAttribute(attributeKey, attributeValue);
        RequestHandler rh = new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                Object value =data.attributes.get(attributeKey);
                assertNotNull(value);
                assertEquals(value, attributeValue);
                return new BacktraceResult(data.report, data.report.exception.getMessage(),
                        BacktraceResultStatus.Ok);
            }
        };
        backtraceClient.setOnRequestHandler(rh);
        final Waiter waiter = new Waiter();

        // WHEN
        backtraceClient.send(new Exception(errorMessage), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
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
