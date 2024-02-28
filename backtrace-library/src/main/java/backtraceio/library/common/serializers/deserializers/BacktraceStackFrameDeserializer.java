package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.models.BacktraceStackFrame;

public class BacktraceStackFrameDeserializer implements Deserializable<BacktraceStackFrame> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceStackFrame.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String functionName = "functionName";
        final static String line = "line";
        final static String sourceCode = "sourceCode";
    }
    public BacktraceStackFrame deserialize(JSONObject obj) throws JSONException {
        return new BacktraceStackFrame(
                obj.optString(fieldNameLoader.get(Fields.functionName), null), // TODO: check fallback warning
                obj.optString(fieldNameLoader.get(Fields.line), null), // TODO: check fallback warning  // todo: should be null in case of empty
                obj.optInt(fieldNameLoader.get(Fields.sourceCode))); // TODO: check fallback warning
    }
}

