package backtraceio.library.models.json;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.TypeHelper;
import backtraceio.library.models.Tuple;

public class ReportDataBuilder {

    /**
     * Divide custom user attributes into primitive and complex attributes and add to this object
     *
     * @param attributes client's attributes
     */
    public static Tuple<Map<String, String>, Map<String, Object>> getReportAttribues(Map<String, Object> attributes) {
        HashMap<String, String> reportAttributes = new HashMap<>();
        HashMap<String, Object> reportAnnotations = new HashMap<>();

        if(attributes == null) {
            return new Tuple<>(reportAttributes, reportAnnotations);
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                reportAttributes.put(entry.getKey(), "null");
                continue;
            }
            Class type = value.getClass();
            if (TypeHelper.isPrimitiveOrPrimitiveWrapperOrString(type)) {
                reportAttributes.put(entry.getKey(), value.toString());
            } else {
                reportAnnotations.put(entry.getKey(), value);
            }
        }

        return new Tuple<>(reportAttributes, reportAnnotations);

    }
}
