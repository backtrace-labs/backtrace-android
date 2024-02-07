package backtraceio.library.models;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

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
        BacktraceReport report = new BacktraceReport(new IllegalAccessException("test-message"));

        // WHEN
        BacktraceData backtraceData = new BacktraceData.Builder(context, report, new HashMap<>()).build();

        // THEN
        assertEquals(backtraceData.classifiers, new String[]{"java.lang.IllegalAccessException"});
        assertEquals(backtraceData.report, report);
        assertEquals(backtraceData.attributes.get("classifier"), "java.lang.IllegalAccessException");
    }
}
