package backtraceio.library.models.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BacktraceAttributesTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        BacktraceStaticAttributes.init(context);
    }

    @Test
    public void testConstructorWithReportAndAttributes() {
        // GIVEN
        String message = "test message";
        Map<String, Object> clientAttributes = new HashMap<>();
        clientAttributes.put("custom-attr", "custom-value");
        BacktraceReport report = new BacktraceReport(message);

        // WHEN
        BacktraceAttributes attributes = new BacktraceAttributes(context, report, clientAttributes);

        // THEN
        Map<String, Object> allAttributes = attributes.getAllAttributes();
        assertNotNull(allAttributes);
        
        // Static attributes
        assertTrue(allAttributes.containsKey("application.package"));
        
        // Client attributes
        assertEquals("custom-value", allAttributes.get("custom-attr"));
        
        // Report attributes
        assertEquals(message, allAttributes.get("error.message"));
        
        // Session attribute
        assertTrue(allAttributes.containsKey("application.session"));
        
        // Dynamic attributes
        assertTrue(allAttributes.containsKey("screen.orientation"));
        assertTrue(allAttributes.containsKey("screen.brightness"));
    }

    @Test
    public void testConstructorWithExceptionReport() {
        // GIVEN
        String exceptionMessage = "exception message";
        Exception ex = new Exception(exceptionMessage);
        BacktraceReport report = new BacktraceReport(ex);

        // WHEN
        BacktraceAttributes attributes = new BacktraceAttributes(context, report, null);

        // THEN
        Map<String, Object> allAttributes = attributes.getAllAttributes();
        assertEquals(exceptionMessage, allAttributes.get("error.message"));
        assertEquals(ex.getClass().getCanonicalName(), allAttributes.get("classifier"));
        
        // Complex attributes should contain the exception
        Map<String, Object> complexAttributes = attributes.getComplexAttributes();
        assertTrue(complexAttributes.containsKey("Exception properties"));

        Exception resultException = (Exception) complexAttributes.get("Exception properties");
        assertEquals(ex.getMessage(), resultException.getMessage());
        assertEquals(ex.getStackTrace().length, resultException.getStackTrace().length);

    }

    @Test
    public void testConstructorWithOnlyClientAttributes() {
        // GIVEN
        Map<String, Object> clientAttributes = new HashMap<>();
        clientAttributes.put("only-client", "value");

        // WHEN
        BacktraceAttributes attributes = new BacktraceAttributes(context, clientAttributes);

        // THEN
        Map<String, Object> allAttributes = attributes.getAllAttributes();
        assertEquals("value", allAttributes.get("only-client"));
        assertTrue(allAttributes.containsKey("application.session"));
        
        // Dynamic attributes should NOT be included by default in this constructor 
        // because it calls this(context, null, clientAttributes, false);
        assertTrue(!allAttributes.containsKey("screen.orientation"));
    }

    @Test
    public void testIncludeDynamicAttributesFlag() {
        // GIVEN
        Map<String, Object> clientAttributes = new HashMap<>();

        // WHEN - includeDynamicAttributes = true
        BacktraceAttributes attrWithDynamic = new BacktraceAttributes(context, null, clientAttributes, true);
        
        // WHEN - includeDynamicAttributes = false
        BacktraceAttributes attrWithoutDynamic = new BacktraceAttributes(context, null, clientAttributes, false);

        // THEN
        assertTrue(attrWithDynamic.getAllAttributes().containsKey("screen.orientation"));
        assertTrue(!attrWithoutDynamic.getAllAttributes().containsKey("screen.orientation"));
    }
}
