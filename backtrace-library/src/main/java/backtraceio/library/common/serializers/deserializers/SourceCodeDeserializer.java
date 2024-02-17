package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.SourceCode;

public class SourceCodeDeserializer  implements Deserializable<SourceCode> {
    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(SourceCode.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String startLine = "startLine";
        final static String sourceCodeFileName = "sourceCodeFileName";
    }
    public SourceCode deserialize(JSONObject obj) throws JSONException {
        return new SourceCode(
                obj.optInt(fieldNameLoader.get(Fields.startLine)),
                obj.optString(fieldNameLoader.get(Fields.sourceCodeFileName))
        );
    }
}
