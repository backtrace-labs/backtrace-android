package backtraceio.library.common.json;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.SummedEventsPayload;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.models.metrics.UniqueEventsPayload;

@RunWith(Parameterized.class)
public class SerializerGsonComparisonTest {
    private final Object object;

    private final static int MAX_INCREASE_TIME_RATIO = 10;

    public SerializerGsonComparisonTest(Object object) {
        this.object = object;
    }

    public static SummedEventsPayload getSummedEventsPayload() {
        final ConcurrentLinkedDeque<SummedEvent> queue = new ConcurrentLinkedDeque<>();
        queue.add(new SummedEvent("sample-name", 123, new HashMap<String, String>() {{
            put("attr-1", "val-1");
            put("attr-2", "val-2");
        }}));

        queue.add(new SummedEvent("test", 1738703564, null));

        return new SummedEventsPayload(queue, "example-app", "v1.0-dev");
    }

    public static UniqueEventsPayload getUniqueEventsPayload() {
        final ConcurrentLinkedDeque<UniqueEvent> queue = new ConcurrentLinkedDeque<>();
        queue.add(new UniqueEvent("sample-name", 123, new HashMap<String, String>() {{
            put("attr-1", "val-1");
            put("attr-2", "val-2");
        }}));

        queue.add(new UniqueEvent("test", 1738703564, null));

        return new UniqueEventsPayload(queue, "example-app", "v1.0-dev");
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new BacktraceApiResult("123", "example-response")},
                {new BacktraceResult("123", "Ok")},
                {new BacktraceReport("message",
                        new HashMap<String, Object>() {{
                            put("1", "1");
                            put("2", "2");
                        }},
                        new ArrayList<String>() {{
                            add("1");
                            add("2");
                        }}
                )},
                {new BacktraceReport(new Exception("test"))},
                {BacktraceStackFrame.fromStackTraceElement(new StackTraceElement("class", "method", "file", 1))},
                {new SourceCode(1, "test")},
                {new ThreadInformation("main", false, new ArrayList<BacktraceStackFrame>() {{
                    add(BacktraceStackFrame.fromStackTraceElement(new StackTraceElement("class", "method", "file", 1)));
                }})},
                {getSummedEventsPayload()},
                {getUniqueEventsPayload()}
        });
    }

    @Test
    public void testGsonPerformanceSerializer() {
        final long norm = 1;
        assertNotNull(object);

        // WHEN ORG.JSON
        final long startTimeBacktrace = System.currentTimeMillis();
        final Object backtraceResult = BacktraceSerializeHelper.toJson(object);
        final long endTimeBacktrace = System.currentTimeMillis();
        final long totalTimeBacktrace = endTimeBacktrace - startTimeBacktrace + norm;

        // WHEN GSON
        final long startTimeGson = System.currentTimeMillis();
        final Object gsonResult = new Gson().toJson(object);
        final long endTimeGson = System.currentTimeMillis();
        final long totalTimeGson = endTimeGson - startTimeGson + norm;

        // THEN
        assertNotNull(backtraceResult);
        assertNotNull(gsonResult);

        System.out.println("Total serialization time of object type " + object.getClass().getSimpleName() + " [GSON]: " + totalTimeGson + " milliseconds, [Org.json]: " + totalTimeBacktrace + " milliseconds");

        assertTrue(totalTimeGson * MAX_INCREASE_TIME_RATIO > totalTimeBacktrace);
    }
}
