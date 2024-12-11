package backtraceio.library.common.json.deserialization;

import org.json.JSONException;
import org.json.JSONObject;

public interface Deserializable<T> {
    T deserialize(JSONObject obj) throws JSONException;
}
