package backtraceio.library.models.metrics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;

public class SummedEventsPayloadTest {

    private final String JSON_FILE = "summedEvents.json";

    @Test
    public void serializeUniqueEventsPayload() {
        // GIVEN
        final ConcurrentLinkedDeque<SummedEvent> events = new ConcurrentLinkedDeque<SummedEvent>() {{
            add(new SummedEvent(
                    "example-event",
                    1734877802,
                    new HashMap<String, String>() {{
                        put("application.session", "3521f3e8-f463-4f6c-90f3-b0771ba67a56");
                        put("screen.height", "1834");
                        put("build.type", "Debug");
                    }}));
        }};
        final String application = "Backtrace.IO";
        final String appVersion = "1.0";
        final SummedEventsPayload obj = new SummedEventsPayload(events, application, appVersion);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, JSON_FILE);
        assertTrue(TestUtils.compareJson(json, expectedJson));
    }
}
