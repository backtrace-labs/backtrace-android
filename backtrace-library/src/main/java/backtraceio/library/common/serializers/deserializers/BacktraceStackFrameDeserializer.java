package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.common.serializers.deserializers.cache.JSONObjectExtensions;
import backtraceio.library.models.BacktraceStackFrame;

public class BacktraceStackFrameDeserializer implements Deserializable<BacktraceStackFrame> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceStackFrame.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String functionName = "functionName";
        final static String line = "line";
        final static String sourceCodeFileName = "sourceCodeFileName";

        final static String sourceCode = "sourceCode";
    }
    public BacktraceStackFrame deserialize(JSONObject obj) throws JSONException {
        return new BacktraceStackFrame(
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.functionName)),
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.sourceCodeFileName)),
                obj.optInt(fieldNameLoader.get(Fields.line)), // TODO: check fallback warning
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.sourceCode)));
    }
}

