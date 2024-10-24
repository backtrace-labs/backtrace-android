package backtraceio.library;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.models.json.BacktraceReport;


public class ConcatAttributesUnitTest {

    private final Map<String, Object> attributesReport = new HashMap<String, Object>() {{
        put("1", "1");
        put("2", "2");
    }};

    private final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put("3", "3");
        put("4", "4");
    }};

    @Test
    public void concatAttributes_isCorrect() {
        // GIVEN
        // 1 - backtraceReport by default adds error.type attribute
        final int expectedAttributesSize = 1 + attributes.size() + attributesReport.size();
        final BacktraceReport report = new BacktraceReport("test", attributesReport, null);
        // WHEN
        final Map<String, Object> result = BacktraceReport.concatAttributes(report, attributes);
        // THEN
        assertEquals(expectedAttributesSize, result.size());
        assertEquals(result.get("2"), "2");
        assertEquals(result.get("4"), "4");
    }

    @Test
    public void concatAttributesNullParam_isCorrect() {
        // GIVEN
        // 1 - backtraceReport by default adds error.type attribute
        int expectedAttributesSize = 1 + attributesReport.size();
        final BacktraceReport report = new BacktraceReport("test", attributesReport, null);
        // WHEN
        final Map<String, Object> result = BacktraceReport.concatAttributes(report, null);
        // THEN
        assertEquals(expectedAttributesSize, result.size());
        assertEquals(result.get("2"), "2");
    }

    @Test
    public void concatAttributesNullAttributes_isCorrect() {
        // GIVEN
        // 1 - backtraceReport by default adds error.type attribute
        final int expectedAttributesSize = 1 + attributes.size();
        final BacktraceReport report = new BacktraceReport("test", null, null);
        // WHEN
        final Map<String, Object> result = BacktraceReport.concatAttributes(report, attributes);
        // THEN
        assertEquals(expectedAttributesSize, result.size());
        assertEquals(result.get("4"), "4");
    }
}