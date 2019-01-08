package backtraceio.library;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.models.json.BacktraceReport;

import static org.junit.Assert.assertEquals;


public class ConcatAttributesUnitTest {

    private Map<String, Object> attributesReport = new HashMap<String, Object>() {{
        put("1", "1");
        put("2", "2");
    }};

    private Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("3", "3");
        put("4", "4");
    }};

    @Test
    public void concatAttributes_isCorrect() {
        BacktraceReport report = new BacktraceReport("test", attributesReport, null);
        Map<String, Object> result = BacktraceReport.concatAttributes(report, attributes);
        assertEquals(4, result.size());
        assertEquals(result.get("2"), "2");
        assertEquals(result.get("4"), "4");
    }

    @Test
    public void concatAttributesNullParam_isCorrect() {
        BacktraceReport report = new BacktraceReport("test", attributesReport, null);
        Map<String, Object> result = BacktraceReport.concatAttributes(report, null);
        assertEquals(2, result.size());
        assertEquals(result.get("2"), "2");
    }

    @Test
    public void concatAttributesNullAttributes_isCorrect() {
        BacktraceReport report = new BacktraceReport("test", null, null);
        Map<String, Object> result = BacktraceReport.concatAttributes(report, attributes);
        assertEquals(2, result.size());
        assertEquals(result.get("4"), "4");
    }
}