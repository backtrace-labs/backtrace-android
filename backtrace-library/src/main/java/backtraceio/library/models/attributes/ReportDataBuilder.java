package backtraceio.library.models.attributes;

import java.util.Map;

import backtraceio.library.common.TypeHelper;

public class ReportDataBuilder {

    /**
     * Divide custom user attributes into primitive and complex attributes and add to this object. By default nullable values will be included.
     *
     * @param attributes client's attributes
     * @return Report data attributes divided into attributes and annotations
     */
    public static ReportDataAttributes getReportAttributes(Map<String, Object> attributes) {
        return getReportAttributes(attributes, false);
    }

    /**
     * Divide custom user attributes into primitive and complex attributes and add to this object
     *
     * @param attributes   client's attributes
     * @param skipNull define attributes behavior on nullable value. By default all nullable attributes
     *                     will be included in the report. For some features like metrics, we don't want to send
     *                     nullable values, because they can generate invalid behavior/incorrect information.
     * @return Report data attributes divided into attributes and annotations
     */
    public static ReportDataAttributes getReportAttributes(Map<String, Object> attributes, boolean skipNull) {
        ReportDataAttributes reportDataAttributes = new ReportDataAttributes();

        if (attributes == null) {
            return reportDataAttributes;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                if (!skipNull) {
                    reportDataAttributes.addAttribute(key, null);
                }
                continue;
            }
            if (TypeHelper.isPrimitiveOrPrimitiveWrapperOrString(value.getClass())) {
                reportDataAttributes.addAttribute(key, value.toString());
            } else {
                reportDataAttributes.addAnnotation(key, value);
            }
        }

        return reportDataAttributes;

    }
}
