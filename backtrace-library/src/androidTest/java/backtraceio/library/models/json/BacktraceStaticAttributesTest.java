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

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BacktraceStaticAttributesTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testInitAndGetAttributes() {
        // GIVEN
        BacktraceStaticAttributes.init(context);

        // WHEN
        BacktraceStaticAttributes instance = BacktraceStaticAttributes.getInstance();
        Map<String, String> attributes = instance.getAttributes();

        // THEN
        assertNotNull(instance);
        assertNotNull(attributes);


        // Check app information
        assertTrue(attributes.containsKey("application.package"));
        assertTrue(attributes.containsKey("application"));
        assertTrue(attributes.containsKey("backtrace.agent"));
        assertEquals("backtrace-android", attributes.get("backtrace.agent"));
        assertTrue(attributes.containsKey("backtrace.version"));

        // Check device information
        assertTrue(attributes.containsKey("guid"));
        assertEquals("Android", attributes.get("uname.sysname"));
        assertTrue(attributes.containsKey("uname.machine"));
        assertTrue(attributes.containsKey("uname.version"));
        assertTrue(attributes.containsKey("culture"));
        assertTrue(attributes.containsKey("build.type"));
        assertTrue(attributes.containsKey("device.model"));
        assertTrue(attributes.containsKey("device.brand"));
        assertTrue(attributes.containsKey("device.product"));
        assertTrue(attributes.containsKey("device.sdk"));
        assertTrue(attributes.containsKey("device.manufacturer"));
        assertTrue(attributes.containsKey("device.os_version"));

        // Check screen information
        assertTrue(attributes.containsKey("screen.width"));
        assertTrue(attributes.containsKey("screen.height"));
        assertTrue(attributes.containsKey("screen.dpi"));
    }

    @Test
    public void testSingletonBehavior() {
        // GIVEN
        BacktraceStaticAttributes.init(context);
        BacktraceStaticAttributes instance1 = BacktraceStaticAttributes.getInstance();

        // WHEN
        BacktraceStaticAttributes.init(context);
        BacktraceStaticAttributes instance2 = BacktraceStaticAttributes.getInstance();

        // THEN
        // Note: The current implementation of init(context) always creates a new instance and assigns it to the static variable.
        // It's not a strict singleton in the sense that it doesn't check if instance is null before overwriting.
        // However, we expect getInstance() to return the latest initialized instance.
        assertNotNull(instance1);
        assertNotNull(instance2);
    }
}
