package backtraceio.library.anr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import backtraceio.library.watchdog.BacktraceWatchdogTimeoutException;
import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

public class BacktraceANRSettingsTest {

    @Test
    public void defaultConstructor() {
        // WHEN
        BacktraceANRSettings settings = new BacktraceANRSettings();
        // THEN
        assertEquals(BacktraceANRSettings.DEFAULT_ANR_TIMEOUT, settings.getTimeout());
        assertNull(settings.getOnApplicationNotRespondingEvent());
        assertFalse(settings.isDebug());
    }

    @Test
    public void paramsConstructor() {
        // GIVEN
        int timeout = 2000;
        boolean debug = true;
        OnApplicationNotRespondingEvent event = exception -> {

        };
        // WHEN
        BacktraceANRSettings settings = new BacktraceANRSettings(timeout, event, debug);

        // THEN
        assertEquals(timeout, settings.getTimeout());
        assertEquals(event, settings.getOnApplicationNotRespondingEvent());
        assertEquals(debug, settings.isDebug());
    }

}
