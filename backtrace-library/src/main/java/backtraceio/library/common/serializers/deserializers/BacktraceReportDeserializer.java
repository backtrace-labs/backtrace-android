package backtraceio.library.common.serializers.deserializers;

import static backtraceio.library.common.BacktraceStringHelper.isNullOrEmpty;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceReportDeserializer implements Deserializable<BacktraceReport> {


    @Override
    // TODO: fix all null warnings
    public BacktraceReport deserialize(JSONObject obj) throws JSONException {
        final String uuid = obj.optString("uuid", null);
        final long timestamp = obj.optLong("timestamp", 0);
        final String message = obj.optString("message", null); // TODO fix
        final String classifier = obj.optString("classifier", null);
        final boolean exceptionTypeReport = obj.optBoolean("exception-type-report");
        final Exception exception = getException(obj.optJSONObject("exception")); // TODO: fix
        final Map<String, Object> attributes = new HashMap<>(); // TODO: fix

        final List<String> attachmentPaths = this.getAttachmentList(obj.optJSONArray("attachmentPaths"));
        final List<BacktraceStackFrame> diagnosticStack = this.getBacktraceStackFrame();

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
        return null; // TODO:
    }

    public Map<String, Object> getAttributes(JSONObject obj) {
        return null; // TODO:
    }

    public List<String> getAttachmentList(JSONArray array) {
        if (array == null) {
            return null;
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            result.add(array.optString(i));
        }

        return result;
    }

    public List<BacktraceStackFrame> getBacktraceStackFrame() {
        return null; // TODO
    }
}
