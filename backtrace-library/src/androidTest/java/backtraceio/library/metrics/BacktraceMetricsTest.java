package backtraceio.library.metrics;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.services.BacktraceMetrics;

@RunWith(AndroidJUnit4.class)
public class BacktraceMetricsTest {
    public Context context;
    public BacktraceClient backtraceClient;
    public BacktraceCredentials credentials;
    private final String summedEventName = "activity-changed";
    // existing attribute name in Backtrace
    private final String[] uniqueAttributeName = {"uname.version", "cpu.boottime", "screen.orientation", "battery.state", "device.airplane_mode", "device.sdk", "device.brand", "system.memory.total", "uname.sysname", "application.package"};

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

    @Test
    public void addAttributesSummedEvent() {
        SummedEvent summedEvent = new SummedEvent(summedEventName, null);
        Map<String, Object> attributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        summedEvent.addAttributes(attributes);
        assertEquals("bar", summedEvent.getAttributes().get("foo"));
    }

    @Test
    public void addAttributesUniqueEvent() {
        UniqueEvent uniqueEvent = new UniqueEvent(uniqueAttributeName[0], null);
        Map<String, Object> attributes = new HashMap<String, Object>() {{
            put("foo", "bar");
        }};
        uniqueEvent.update(BacktraceTimeHelper.getTimestampSeconds(), attributes);
        assertEquals("bar", uniqueEvent.getAttributes().get("foo"));
    }

    @Test
    public void testDefaultUrl() {
        // GIVEN
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, credentials);
        BacktraceMetricsSettings settings = new BacktraceMetricsSettings(credentials);
        // WHEN
        metrics.enable(settings);

        // THEN
        TestCase.assertEquals(BacktraceMetrics.defaultBaseUrl, metrics.getBaseUrl());
        TestCase.assertTrue(settings.isBacktraceServer());
    }

    @Test(expected = IllegalStateException.class)
    public void tryToEnableMetricsTwoTimes() {
        // GIVEN
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, credentials);
        BacktraceMetricsSettings settings = new BacktraceMetricsSettings(credentials);
        // WHEN
        metrics.enable(settings);
        metrics.enable(settings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryToEnableMetricsOnCustomServer() {
        // GIVEN
        BacktraceCredentials customCredentials = new BacktraceCredentials("https://custom.on.premise.server.io:6098", token);
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, customCredentials);

        // WHEN
        metrics.enable();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryToAddSummedEventOnCustomServer() {
        // GIVEN
        BacktraceCredentials customCredentials = new BacktraceCredentials("https://custom.on.premise.server.io:6098", token);
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, customCredentials);

        // WHEN
        metrics.addSummedEvent("demo", new HashMap<String, Object>() {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryToAddUniqueEventOnCustomServer() {
        // GIVEN
        BacktraceCredentials customCredentials = new BacktraceCredentials("https://custom.on.premise.server.io:6098", token);
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, customCredentials);

        // WHEN
        metrics.addUniqueEvent("demo", new HashMap<String, Object>() {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryToSendOnCustomServer() {
        // GIVEN
        BacktraceCredentials customCredentials = new BacktraceCredentials("https://custom.on.premise.server.io:6098", token);
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, customCredentials);

        // WHEN
        metrics.send();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tryToSendStartupEventOnCustomServer() {
        // GIVEN
        BacktraceCredentials customCredentials = new BacktraceCredentials("https://custom.on.premise.server.io:6098", token);
        BacktraceMetrics metrics = new BacktraceMetrics(context, new HashMap<>(), null, customCredentials);

        // WHEN
        metrics.sendStartupEvent();
    }

}
