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

        // WHEN
        BacktraceData backtraceData = new BacktraceData.Builder(context, report, new HashMap<>()).build();

        // THEN
        assertArrayEquals(backtraceData.classifiers, new String[]{"java.lang.IllegalAccessException"});
        assertEquals(backtracedata.getReport(), report);
        assertEquals(backtraceData.attributes.get("classifier"), "java.lang.IllegalAccessException");
        assertEquals(backtraceData.agent, "backtrace-android");
        assertEquals(backtraceData.agentVersion, "3.8.0-6-6b6db45-backtrace-data-refactor");
        assertEquals(backtraceData.lang, "java");
        assertEquals(backtraceData.langVersion, "0");
        assertEquals(backtraceData.symbolication, "");
        assertEquals(backtraceData.timestamp, report.timestamp);
        assertEquals(backtraceData.uuid, report.uuid.toString());
        assertEquals(backtraceData.attributes.size(), 40);
        assertEquals(backtraceData.annotations.size(), 3);
        assertEquals(backtraceData.mainThread, "instr: androidx.test.runner.androidjunitrunner");
        assertEquals(backtraceData.sourceCode.size(), 34);
        assertEquals(backtraceData.getThreadInformationMap().size(), 13);
        assertEquals(backtraceData.getAttachmentPaths().size(), 2);
    }
}
