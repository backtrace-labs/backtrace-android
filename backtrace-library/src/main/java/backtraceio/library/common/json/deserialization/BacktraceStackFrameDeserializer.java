package backtraceio.library.common.json.deserialization;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.json.deserialization.cache.FieldNameLoader;
import backtraceio.library.common.json.deserialization.cache.JSONObjectExtensions;
import backtraceio.library.models.BacktraceStackFrame;

public class BacktraceStackFrameDeserializer implements Deserializable<BacktraceStackFrame> {

    private final static FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceStackFrame.class);

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
                JSONObjectExtensions.optIntegerOrNull(obj, fieldNameLoader.get(Fields.line)),
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.sourceCode)));
    }
}
