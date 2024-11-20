package backtraceio.library.models.attributes;

import java.util.HashMap;
import java.util.Map;

public class ReportDataAttributes {
    private final Map<String, String> reportAttributes = new HashMap<>();

    private final Map<String, Object> reportAnnotations = new HashMap<>();


    public void addAnnotation(String key, Object value) {
        reportAnnotations.put(key, value);
    }

    public void addAttribute(String key, String value) {
        reportAttributes.put(key, value);
    }

    public Map<String, String> getAttributes() {
        return reportAttributes;
    }

    public Map<String, Object> getAnnotations() {
        return reportAnnotations;
    }
}
