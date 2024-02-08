package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceResultDeserializer implements Deserializable<BacktraceResult> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceResult.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String rxId = "rxId";
        final static String status = "response";
    }
    public BacktraceResult deserialize(JSONObject obj) throws JSONException {
        return new BacktraceResult(obj.optString(fieldNameLoader.get(Fields.rxId), null), // TODO: check fallback warning
                obj.optString(fieldNameLoader.get(Fields.status), BacktraceResultStatus.Ok.toString())
        );
    }
}
