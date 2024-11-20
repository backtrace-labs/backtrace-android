package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static backtraceio.library.TestUtils.readFileAsString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

import backtraceio.library.common.serializers.BacktraceDeserializer;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataTest {

    @Test
    public void deserializeBacktraceData() throws JSONException {
        // GIVEN
        String backtraceDataJson = readFileAsString(this, "backtraceData2.json");
        // WHEN
        final BacktraceData result = BacktraceDeserializer.deserialize(new JSONObject(backtraceDataJson), BacktraceData.class);

        // THEN
        assertNotNull(result);
        assertEquals("398dad51-7c39-4d64-941c-854de56f5f2b", result.getUuid());
        assertEquals("java", result.getLang());
        assertEquals("backtrace-android", result.getAgent());
        assertEquals("proguard", result.getSymbolication());
        assertEquals(1709680075, result.getTimestamp());
        assertEquals("0", result.getLangVersion());
        assertEquals("3.7.14-1-931f45d", result.getAgentVersion());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", result.getMainThread());
        assertEquals(1, result.getClassifiers().length);
        assertEquals("java.lang.IllegalAccessException", result.getClassifiers()[0]);
        assertNull(result.getReport());

        // THEN getAttributes()
        assertEquals(result.getAttributes().size(), 41);
        assertEquals(result.getAttributes().get("application"), "backtraceio.library.test");
        assertEquals(result.getAttributes().get("error.type"), "Exception");

        // THEN getAnnotations()
        assertEquals(5, result.getAnnotations().size());
        assertEquals("Test", ((Map<String, String>) result.getAnnotations().get("Exception")).get("message"));
        assertNotNull(result.getAnnotations().get("Exception properties"));
        assertEquals("Test", ((Map<String, String>) result.getAnnotations().get("Exception properties")).get("detail-message"));
        assertNotNull(result.getAnnotations().get("1"));
        assertNotNull(result.getAnnotations().get("123"));


        // THEN Source Code
        assertEquals(result.getSourceCode().size(), 35);
        SourceCode firstSourceCode = result.getSourceCode().get("ca0a50a1-d553-4479-8fe2-28c3f527743b");

        assertNotNull(firstSourceCode);
        assertEquals(firstSourceCode.sourceCodeFileName, "ParentRunner.java");
        assertEquals(firstSourceCode.startLine.intValue(), 306);

        // THEN Thread information
        assertEquals(result.getThreadInformationMap().size(), 13);
        ThreadInformation threadInformation = result.getThreadInformationMap().get("finalizerwatchdogdaemon");

        assertNotNull(threadInformation);
        assertEquals(threadInformation.getFault(), false);
        assertEquals(threadInformation.getName(), "finalizerwatchdogdaemon");
        assertNotNull(threadInformation.getStack());
        assertEquals(threadInformation.getStack().get(1).sourceCode, "4f359140-a606-4f5d-ba67-adb387383d43");
        assertEquals(threadInformation.getStack().get(1).functionName, "java.lang.Object.wait");
        assertEquals(threadInformation.getStack().get(1).line.intValue(), 442);
    }

    // TODO: add one more complicated example with classifier
}
