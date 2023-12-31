package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionDeserializer implements Deserializable<Exception> {
    @Override
    public Exception deserialize(JSONObject obj) throws JSONException {
        return null; // TODO: fix
    }
}
