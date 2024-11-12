package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.common.serializers.deserializers.cache.JSONObjectExtensions;
import backtraceio.library.models.json.SourceCode;

public class SourceCodeDeserializer implements Deserializable<SourceCode> {
    private final static FieldNameLoader fieldNameLoader = new FieldNameLoader(SourceCode.class);
    static class Fields {
        final static String startLine = "startLine";
        final static String sourceCodeFileName = "sourceCodeFileName";
    }
    public SourceCode deserialize(JSONObject obj) throws JSONException {
        return new SourceCode(
                JSONObjectExtensions.optIntegerOrNull(obj, fieldNameLoader.get(Fields.startLine)),
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.sourceCodeFileName))
        );
    }
}
