package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceDataDeserializer implements Deserializable<BacktraceData>{

    public BacktraceData deserialize(JSONObject obj) throws JSONException {
        return new BacktraceData(); // TODO
    }
}
