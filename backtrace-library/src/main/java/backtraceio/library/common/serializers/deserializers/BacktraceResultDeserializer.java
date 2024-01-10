package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceResultDeserializer implements Deserializable<BacktraceResult> {

    public BacktraceResult deserialize(JSONObject obj) throws JSONException {
        return new BacktraceResult(obj.optString("_rxid", null),
                obj.optString("response", BacktraceResultStatus.Ok.toString())
        );
    }
}
