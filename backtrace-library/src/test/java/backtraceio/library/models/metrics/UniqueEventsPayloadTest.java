package backtraceio.library.models.metrics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;

public class UniqueEventsPayloadTest {

    private final String JSON_FILE = "uniqueEvents.json";
    @Test
    public void serializeUniqueEventsPayload() {
        // GIVEN
        final ConcurrentLinkedDeque<UniqueEvent> events = new ConcurrentLinkedDeque<UniqueEvent>() {{
            add(new UniqueEvent("event-name", 1734877802, new HashMap<String, String>() {{
                put("application.session", "3521f3e8-f463-4f6c-90f3-b0771ba67a56");
                put("screen.height", "1834");
                put("build.type", "Debug");
            }}));
        }};
        final String application = "Backtrace.IO";
        final String appVersion = "1.0";
        final UniqueEventsPayload obj = new UniqueEventsPayload(events, application, appVersion);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, JSON_FILE);
        assertTrue(TestUtils.compareJson(json, expectedJson));
    }
}
