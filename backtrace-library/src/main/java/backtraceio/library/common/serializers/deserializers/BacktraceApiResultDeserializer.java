package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.common.serializers.deserializers.cache.JSONObjectExtensions;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceApiResultDeserializer implements Deserializable<BacktraceApiResult> {

    private final static FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceApiResult.class);

    static class Fields {
        final static String rxId = "rxId";
        final static String response = "response";
    }
    public BacktraceApiResult deserialize(JSONObject obj) throws JSONException {
        return new BacktraceApiResult(
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.rxId)),
                obj.optString(fieldNameLoader.get(Fields.response), BacktraceResultStatus.ServerError.toString())
        );
    }
}
