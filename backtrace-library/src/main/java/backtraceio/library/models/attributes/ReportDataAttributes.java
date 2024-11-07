package backtraceio.library.models.attributes;

import java.util.HashMap;

public class ReportDataAttributes {
    private final HashMap<String, String> reportAttributes = new HashMap<>();

    private final HashMap<String, Object> reportAnnotations = new HashMap<>();


    public void addAnnotation(String key, Object value) {
        reportAnnotations.put(key, value);
    }

    public void addAttribute(String key, String value) {
        reportAttributes.put(key, value);
    }

    public HashMap<String, String> getAttributes() {
        return reportAttributes;
    }

    public HashMap<String, Object> getAnnotations() {
        return reportAnnotations;
    }

}
