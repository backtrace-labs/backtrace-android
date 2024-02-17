package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

// TODO: check if methods should be private
public class BacktraceDataDeserializer implements Deserializable<BacktraceData>{

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceData.class); // TODO: maybe we can reuse it
    static class Fields {
        final static String uuid = "uuid";
        final static String symbolication = "symbolication";
        final static String timestamp = "timestamp";
        final static String langVersion = "langVersion";
        final static String agentVersion = "agentVersion";
        final static String attributes = "attributes";
        final static String mainThread = "mainThread";
        final static String classifiers = "classifiers";
        final static String annotations = "annotations";
        final static String sourceCode = "sourceCode";
        final static String report = "report";
        final static String threadInformationMap = "threadInformationMap";

    }
    public BacktraceData deserialize(JSONObject obj) throws JSONException {
        return new BacktraceData(
              obj.optString(fieldNameLoader.get(Fields.uuid)),
                obj.optString(fieldNameLoader.get(Fields.symbolication)),
                obj.optLong(fieldNameLoader.get(Fields.timestamp)),
                obj.optString(fieldNameLoader.get(Fields.langVersion)), // TODO: fix or improve casing should get from annotation
                obj.optString(fieldNameLoader.get(Fields.agentVersion)), // TODO: fix or improve casing should get from annotation
                // TODO: fix all below and maybe add fallback
                getAttributes(obj.optJSONObject(fieldNameLoader.get(Fields.attributes))),
                obj.optString(fieldNameLoader.get(Fields.mainThread)),
                getClassifiers(obj.optJSONArray(fieldNameLoader.get(Fields.classifiers))),
                getBacktraceReport(obj.optJSONObject(fieldNameLoader.get(Fields.report))),
                getAnnotations(obj.optJSONObject(fieldNameLoader.get(Fields.annotations))),
                getSourceCode(obj.optJSONObject(fieldNameLoader.get(Fields.sourceCode))),
                getThreadInformation(obj.optJSONObject(fieldNameLoader.get(Fields.threadInformationMap)))
        ); // TODO
    }

    public BacktraceReport getBacktraceReport(JSONObject obj) throws JSONException {
        // TODO: reuse deserializer ?
        BacktraceReportDeserializer deserializer = new BacktraceReportDeserializer();
        return deserializer.deserialize(obj);
    }

    public Map<String, Object> getAnnotations(JSONObject obj) {
        // TODO: implement
        return null;
    }

    public Map<String, SourceCode> getSourceCode(JSONObject obj)  {
        SourceCodeDeserializer deserializer = new SourceCodeDeserializer();

        Map<String, SourceCode> result = new HashMap<>();

        Iterator<String> keys = obj.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                if (obj.get(key) instanceof JSONObject) {
                    result.put(key, deserializer.deserialize(obj));
                }
            } catch (JSONException e) {
                // TODO:
            }
        }
        return result;
    }

    public Map<String, ThreadInformation> getThreadInformation(JSONObject obj) {
        // TODO: implement
        // TODO: what if obj == null
        Map<String, ThreadInformation> result = new HashMap<>();
        final ThreadInformationDeserializer deserializer = new ThreadInformationDeserializer();
        Iterator<String> keys = obj.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                if (obj.get(key) instanceof JSONObject) {
                    result.put(key, deserializer.deserialize(obj));
                }
            } catch (JSONException e) {
                // TODO:
            }
        }
        return result;
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
