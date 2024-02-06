package backtraceio.library.common.serializers.deserializers;

import static backtraceio.library.common.BacktraceStringHelper.isNullOrEmpty;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceReportDeserializer implements Deserializable<BacktraceReport> {

    ExceptionDeserializer exceptionDeserializer;
    public BacktraceReportDeserializer() {
        this.exceptionDeserializer = new ExceptionDeserializer();
    }
    @Override
    // TODO: fix all null warnings
    public BacktraceReport deserialize(JSONObject obj) throws JSONException {
        final String uuid = obj.optString("uuid", null);
        final long timestamp = obj.optLong("timestamp", 0);
        final String message = obj.optString("message", null);
        final String classifier = obj.optString("classifier", "");
        final boolean exceptionTypeReport = obj.optBoolean("exception-type-report");
        final Exception exception = getException(obj.optJSONObject("exception"));
        final Map<String, Object> attributes = this.getAttributes(obj.optJSONObject("attributes")); // TODO: fix

        final List<String> attachmentPaths = this.getAttachmentList(obj.optJSONArray("attachmentPaths"));
        final List<BacktraceStackFrame> diagnosticStack = this.getDiagnosticStack(obj.optJSONArray("diagnostic-stack")); // todo fix diagnostic-stack name

        return new BacktraceReport(
                !isNullOrEmpty(uuid)? UUID.fromString(uuid) : null,
                timestamp,
                exceptionTypeReport,
                classifier,
                attributes,
                message,
                exception,
                attachmentPaths,
                diagnosticStack
        );
    }

    public Exception getException(JSONObject obj) {
        return this.exceptionDeserializer.deserialize(obj); // TODO: fix usage
    }

    public Map<String, Object> getAttributes(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        Iterator<String> attributesKeys = obj.keys();
        while (attributesKeys.hasNext()) {
            String key = attributesKeys.next();
            result.put(key, obj.optString(key));
        }

        return result;
    }

    public List<String> getAttachmentList(JSONArray array) {
        if (array == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            result.add(array.optString(i));
        }

        return result;
    }

    public List<BacktraceStackFrame> getDiagnosticStack(JSONArray obj) {
        if (obj == null) {
            return null;
        }

        List<BacktraceStackFrame> result = new ArrayList<>();
        for (int i = 0; i < obj.length(); i++) {
            JSONObject stackItem = obj.optJSONObject(i);
            if (stackItem != null) {
                BacktraceStackFrame stackFrame = new BacktraceStackFrame();
                stackFrame.functionName = stackItem.optString("function-name");
                stackFrame.line = stackItem.optInt("line"); // todo: should be null in case of empty
                stackFrame.sourceCode = stackItem.optString("source-code");
                result.add(stackFrame);
            }
        }

        return result;
    }
}
