package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

public interface Deserializable<T> {
    public <T> T deserialize(JSONObject jsonObj) throws JSONException;
}
