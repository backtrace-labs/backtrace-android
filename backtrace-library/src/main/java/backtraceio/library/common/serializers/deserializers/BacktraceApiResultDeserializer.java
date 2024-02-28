package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceApiResultDeserializer implements Deserializable<BacktraceApiResult> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceApiResult.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String rxId = "rxId";
        final static String response = "response";
    }
    public BacktraceApiResult deserialize(JSONObject obj) throws JSONException {
        return new BacktraceApiResult(
                obj.optString(fieldNameLoader.get(Fields.rxId), null), // TODO: check fallback warning
                obj.optString(fieldNameLoader.get(Fields.response), BacktraceResultStatus.Ok.toString())
        );
    }
}
