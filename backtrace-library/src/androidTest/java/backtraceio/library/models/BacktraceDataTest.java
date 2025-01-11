package backtraceio.library.models;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.common.json.naming.NamingPolicy;
import backtraceio.library.common.json.serialization.BacktraceDataSerializer;
import backtraceio.library.models.json.BacktraceReport;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void createBacktraceDataBuilderTest() {
        // GIVEN
        List<String> attachmentsPath = Arrays.asList("one", "two", "three");
        BacktraceReport report = new BacktraceReport(new IllegalAccessException("test-message"), attachmentsPath);

        Map<String, Object> clientAttributes = new HashMap<>();
        clientAttributes.put("attr-1", 1);
        clientAttributes.put("attr-2", true);
        clientAttributes.put("attr-3", "test");
        // WHEN
        BacktraceData backtraceData = new BacktraceData.Builder(report).setAttributes(context, clientAttributes).build();

        // THEN
        assertArrayEquals( new String[]{"java.lang.IllegalAccessException"} , backtraceData.getClassifiers());
        assertEquals(report, backtraceData.getReport());
        assertEquals("java.lang.IllegalAccessException", backtraceData.getAttributes().get("classifier"));
        assertEquals("backtrace-android", backtraceData.getAgent());
        assertEquals(backtraceio.library.BuildConfig.VERSION_NAME, backtraceData.getAgentVersion());
        assertEquals("java", backtraceData.getLang());
        assertEquals("0", backtraceData.getLangVersion());
        assertEquals("", backtraceData.getSymbolication());
        assertEquals(report.timestamp, backtraceData.getTimestamp());
        assertEquals(report.uuid.toString(), backtraceData.getUuid());
        assertEquals(43, backtraceData.getAttributes().size());
        assertEquals(3, backtraceData.getAnnotations().size());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", backtraceData.getMainThread());
        assertEquals(34, backtraceData.getSourceCode().size());
        assertTrue(!backtraceData.getThreadInformationMap().isEmpty());
        assertEquals(3, backtraceData.getAttachmentPaths().size());
        assertEquals("1", backtraceData.getAttributes().get("attr-1"));
        assertEquals("true", backtraceData.getAttributes().get("attr-2"));
        assertEquals("test", backtraceData.getAttributes().get("attr-3"));
    }

    public BacktraceData createBacktraceDataObj(BacktraceReport report) {
        Map<String, Object> clientAttributes = new HashMap<>();
        clientAttributes.put("attr-1", 1);
        clientAttributes.put("attr-2", true);
        clientAttributes.put("attr-3", "test");

        // WHEN
        return new BacktraceData(context, report, clientAttributes);
    }
    @Test
    public void testBacktraceDataConstructor() {
        // GIVEN
        List<String> attachmentsPath = Arrays.asList("one", "two", "three");
        BacktraceReport report = new BacktraceReport(new IllegalAccessException("test-message"), attachmentsPath);
        BacktraceData backtraceData = createBacktraceDataObj(report);

        // THEN
        assertArrayEquals( new String[]{"java.lang.IllegalAccessException"}, backtraceData.getClassifiers());
        assertEquals(report, backtraceData.getReport());
        assertEquals("java.lang.IllegalAccessException", backtraceData.getAttributes().get("classifier"));
        assertEquals("backtrace-android", backtraceData.getAgent());
        assertEquals(backtraceio.library.BuildConfig.VERSION_NAME, backtraceData.getAgentVersion());
        assertEquals("java", backtraceData.getLang());
        assertEquals("0", backtraceData.getLangVersion());
        assertEquals("", backtraceData.getSymbolication());
        assertEquals(report.timestamp, backtraceData.getTimestamp());
        assertEquals(report.uuid.toString(), backtraceData.getUuid());
        assertEquals(43, backtraceData.getAttributes().size());
        assertEquals(3, backtraceData.getAnnotations().size());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", backtraceData.getMainThread());
        assertEquals(34, backtraceData.getSourceCode().size());
        assertFalse(backtraceData.getThreadInformationMap().isEmpty());
        assertEquals(3, backtraceData.getAttachmentPaths().size());
        assertEquals("1", backtraceData.getAttributes().get("attr-1"));
        assertEquals("true", backtraceData.getAttributes().get("attr-2"));
        assertEquals("test", backtraceData.getAttributes().get("attr-3"));
    }
    
    @Test
    public void serializeBacktraceData() throws JSONException {
        // GIVEN
        List<String> attachmentsPath = Arrays.asList("one", "two", "three");
        BacktraceReport report = new BacktraceReport(new IllegalAccessException("test-message"), attachmentsPath);
        BacktraceData backtraceData = createBacktraceDataObj(report);

        // WHEN
        JSONObject json = new BacktraceDataSerializer(new NamingPolicy()).toJson(backtraceData);

        // THEN
        assertNotNull(json);
        assertEquals("java", json.getString("lang"));
        assertEquals("backtrace-android", json.getString("agent"));
        assertEquals("", json.getString("symbolication"));
        assertEquals(36, json.getString("uuid").length());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", json.getString("mainThread"));
        assertEquals(1, json.getJSONArray("classifiers").length());
        assertEquals("java.lang.IllegalAccessException", json.getJSONArray("classifiers").get(0));
        assertTrue(json.getLong("timestamp") > 0);
        assertNotNull(json.getJSONObject("attributes"));
        assertNotNull(json.getJSONObject("annotations"));
        assertNotNull(json.getJSONObject("sourceCode"));
        assertNotNull(json.getJSONObject("threads"));
    }
}
