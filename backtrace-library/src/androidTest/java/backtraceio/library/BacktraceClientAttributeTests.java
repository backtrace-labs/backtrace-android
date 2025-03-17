package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientAttributeTests {
    private Context context;
    private BacktraceCredentials credentials;
    private BacktraceDatabase database;

    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
        database = new BacktraceDatabase(context, context.getFilesDir().getAbsolutePath());
        backtraceClient = new BacktraceClient(context, credentials, database);
    }

    @After
    public void cleanUp() {
        database.clear();
    }

    @Test
    public void shouldAddASingleAttribute() {
        // GIVEN
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";

        // WHEN
        backtraceClient.addAttribute(attributeKey, attributeValue);

        // THEN
        final Map<String, Object> attributes = backtraceClient.getAttributes();

        final Object value = attributes.get(attributeKey);
        assertNotNull(value);
        assertEquals(value, attributeValue);
    }

    @Test
    public void shouldAddMultipleAttributesAtOnce() {
        // GIVEN
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";
        final Integer numberOfAttributesToVerify = 3;
        final Map<String, Object> attributes = new HashMap<>();
        for (int attributeIteration = 0; attributeIteration < numberOfAttributesToVerify; attributeIteration++) {
            attributes.put(String.format("%s %d", attributeKey, attributeIteration), attributeValue);
        }
        // WHEN
        backtraceClient.addAttribute(attributes);

        // THEN
        for (int attributeIteration = 0; attributeIteration < numberOfAttributesToVerify; attributeIteration++) {
            final Object value = attributes.get(String.format("%s %d", attributeKey, attributeIteration));
            assertNotNull(value);
            assertEquals(value, attributeValue);
        }
    }

    @Test
    public void shouldReplaceExistingAttribute() {
        // GIVEN
        final String attributeKey = "test-attribute";
        final String oldAttributeValue = "old-test-value";
        final String newAttributeValue = "test-value-new";

        // WHEN
        backtraceClient.addAttribute(attributeKey, oldAttributeValue);

        backtraceClient.addAttribute(attributeKey, newAttributeValue);

        // THEN
        final Map<String, Object> attributes = backtraceClient.getAttributes();

        final Object value = attributes.get(attributeKey);
        assertNotNull(value);
        assertEquals(value, newAttributeValue);
    }

    @Test
    public void shouldReplaceExistingAttributePassingUnmodifiableMap() {
        // GIVEN
        final String attributeKey = "test-attribute";
        final String oldAttributeValue = "old-test-value";
        final String newAttributeValue = "test-value-new";

        final Map<String, Object> map = new HashMap<>();
        map.put(attributeKey, oldAttributeValue);

        final Map<String, Object> unmodifiableMap = Collections.unmodifiableMap(map);

        // WHEN
        final BacktraceClient backtraceClient = new BacktraceClient(context, credentials, unmodifiableMap);
        backtraceClient.addAttribute(attributeKey, oldAttributeValue);
        backtraceClient.addAttribute(attributeKey, newAttributeValue);

        // THEN
        final Map<String, Object> attributes = backtraceClient.getAttributes();
        final Object value = attributes.get(attributeKey);

        assertNotNull(value);
        assertEquals(value, newAttributeValue);
    }

    @Test
    public void attributesShouldBeAvailableInReport() {
        // GIVEN
        final String errorMessage = "error message";
        final String attributeKey = "test-attribute";
        final String attributeValue = "test-value";

        backtraceClient.addAttribute(attributeKey, attributeValue);
        RequestHandler rh = data -> {
            // THEN
            Object value = data.getAttributes().get(attributeKey);
            assertNotNull(value);
            assertEquals(value, attributeValue);
            return new BacktraceResult(data.getReport(), data.getReport().exception.getMessage(),
                    BacktraceResultStatus.Ok);
        };
        backtraceClient.setOnRequestHandler(rh);
        final Waiter waiter = new Waiter();

        // WHEN
        backtraceClient.send(new Exception(errorMessage), backtraceResult -> waiter.resume()
        );
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

}
