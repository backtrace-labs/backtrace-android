package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceDataDeserializer implements Deserializable<BacktraceData>{

    public BacktraceData deserialize(JSONObject obj) throws JSONException {

        return new BacktraceData(
              obj.optString("uuid"),
                obj.optString("symbolication"),
                obj.optLong("timestamp"),
                obj.optString("lang-version"), // TODO: fix or improve casing should get from annotation
                obj.optString("agent-version"), // TODO: fix or improve casing should get from annotation
                // TODO: fix all below
                null,
                obj.optString("main-thread"),
                null,
                null,
                null,
                null,
                null
        ); // TODO
    }
}
