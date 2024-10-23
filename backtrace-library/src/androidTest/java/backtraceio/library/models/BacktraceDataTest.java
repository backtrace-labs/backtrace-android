package backtraceio.library.models;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.json.BacktraceReport;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void createBacktraceDataTest() {
        // GIVEN
        List<String> attachmentsPath = Arrays.asList("one", "two", "three");
        BacktraceReport report = new BacktraceReport(new IllegalAccessException("test-message"), attachmentsPath);

        Map<String, Object> clientAttributes = new HashMap<>();
        clientAttributes.put("attr-1", 1);
        clientAttributes.put("attr-2", true);
        clientAttributes.put("attr-3", "test");
        // WHEN
        BacktraceData backtraceData = new BacktraceData.Builder(context, report, clientAttributes).build();

        // THEN
        assertArrayEquals(backtraceData.getClassifiers(), new String[]{"java.lang.IllegalAccessException"});
        assertEquals(backtraceData.getReport(), report);
        assertEquals(backtraceData.getAttributes().get("classifier"), "java.lang.IllegalAccessException");
        assertEquals(backtraceData.getAgent(), "backtrace-android");
        assertEquals(backtraceData.getAgentVersion(), backtraceio.library.BuildConfig.VERSION_NAME);
        assertEquals(backtraceData.getLang(), "java");
        assertEquals(backtraceData.getLangVersion(), "0");
        assertEquals(backtraceData.getSymbolication(), "");
        assertEquals(backtraceData.getTimestamp(), report.timestamp);
        assertEquals(backtraceData.getUuid(), report.uuid.toString());
        assertEquals(backtraceData.getAttributes().size(), 43);
        assertEquals(backtraceData.getAnnotations().size(), 3);
        assertEquals(backtraceData.getMainThread(), "instr: androidx.test.runner.androidjunitrunner");
        assertEquals(backtraceData.getSourceCode().size(), 34);
        assertEquals(backtraceData.getThreadInformationMap().size(), 13);
        assertEquals(backtraceData.getAttachmentPaths().size(), 3);
        assertEquals(backtraceData.getAttributes().get("attr-1"), "1");
        assertEquals(backtraceData.getAttributes().get("attr-2"), "true");
        assertEquals(backtraceData.getAttributes().get("attr-3"), "test");
    }

    // TODO: Add test with BacktraceData constructor
}
