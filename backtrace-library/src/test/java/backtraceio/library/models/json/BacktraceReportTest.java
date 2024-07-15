package backtraceio.library.models.json;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceStackFrame;

public class BacktraceReportTest {
    private final String JSON_FILE = "backtraceReport.json";
    @Test
    public void serialize() {
        // GIVEN
//           public BacktraceReport(UUID uuid, long timestamp,
//        boolean exceptionTypeReport, String classifier,
//                Map<String, Object> attributes,
//                String message, Exception exception,
//                List<String> attachmentPaths,
//                List<BacktraceStackFrame> diagnosticStack) {
//
//        }
        List<BacktraceStackFrame> diagnosticStack = new ArrayList<>();

        diagnosticStack.add(new BacktraceStackFrame("backtraceio.library.SettingAttributesTest.tmpGsonTest", null, 75, "c37b9ae3-eab1-4928-9533-f1c14b6149f5"));
        diagnosticStack.add(new BacktraceStackFrame("java.lang.reflect.Method.invoke", null, null, "6f280747-feee-4f4b-9eff-dda0d8eaa535"));
        Exception exception = new IllegalAccessException();
//        exception.
        StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = new StackTraceElement("backtraceio.library.SettingAttributesTest", "tmpGsonTest", "SettingAttributesTest.java", 75);

        exception.setStackTrace(stackTraceElements);
        BacktraceReport report = new BacktraceReport(UUID.fromString("a62a533a-a7b8-415c-9a99-253c51f00827"), 1709680251, true, "java.lang.IllegalAccessException", null, null, exception, new ArrayList<String>() {{ add("abc.txt"); }}, diagnosticStack);
//        BacktraceReport report = new BacktraceReport(new IllegalAccessException(), null, new ArrayList<String>() {{ add("abc.txt"); }});

//        report.diagnosticStack.


//        report.uuid

        // WHEN
        String json = BacktraceSerializeHelper.toJson(report);
        // THEN

        String expectedJson = TestUtils.unifyJsonString(
                TestUtils.readFileAsString(this, JSON_FILE)
        );

//        assertTrue(TestUtils.compareJson(json, expectedJson));
        Gson g = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> firstMap = g.fromJson(json, mapType);
        Map<String, Object> secondMap = g.fromJson(expectedJson, mapType);
        System.out.println(Maps.difference(firstMap, secondMap));
//        assertNotNull(json);
    }

    @Test
    public void deserialize() {

    }

    @Test
    public void serializeAndDeserialize() {

    }
}
