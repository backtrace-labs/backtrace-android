package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

// TODO: check if methods should be private
public class BacktraceDataDeserializer implements Deserializable<BacktraceData>{

    public BacktraceData deserialize(JSONObject obj) throws JSONException {
        return new BacktraceData(
              obj.optString("uuid"),
                obj.optString("symbolication"),
                obj.optLong("timestamp"),
                obj.optString("lang-version"), // TODO: fix or improve casing should get from annotation
                obj.optString("agent-version"), // TODO: fix or improve casing should get from annotation
                // TODO: fix all below
                getAttributes(obj.optJSONObject("attributes")),
                obj.optString("main-thread"),
                // TODO: deserialize all of below
                getClassifiers(obj.optJSONArray("classifiers")),
                getBacktraceReport(obj.optJSONObject("report")),
                getAnnotations(obj.optJSONObject("annotations")),
                getSourceCode(obj.optJSONObject("sourceCode")),

                getThreadInformation(obj.optJSONObject("threads"))
        ); // TODO
    }

    public BacktraceReport getBacktraceReport(JSONObject obj) {
        // TODO: implement
        return null;
    }

    public Map<String, Object> getAnnotations(JSONObject obj) {
        // TODO: implement
        return null;
    }

    public Map<String, SourceCode> getSourceCode(JSONObject obj) {
        // TODO: implement
        return null;
    }

    public Map<String, ThreadInformation> getThreadInformation(JSONObject obj) {
        // TODO: implement
        return null;
    }

    public String [] getClassifiers(JSONArray jsonArray) {

        if (jsonArray == null) {
            return new String[0];
        }

        String[] result = new String[jsonArray.length()];

        for(int i = 0; i < jsonArray.length(); i++){
            try {
                Object object = jsonArray.get(i);
                result[i] = object.toString();
            }
            catch (JSONException exception) {
                // todo: error handling
            }
        }

        return result;
    }

    public Map<String, String> getAttributes(JSONObject obj) {
        Map<String, String> result = new HashMap<>();

        if (obj == null) {
            return result;
        }

        Iterator<String> keys = obj.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                result.put(key, obj.get(key).toString());
            } catch (JSONException e) {
                // TODO: error handling
            }
        }

        return result;
    }
}
