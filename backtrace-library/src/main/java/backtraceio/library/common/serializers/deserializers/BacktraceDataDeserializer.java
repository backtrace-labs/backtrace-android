package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import backtraceio.library.common.serializers.BacktraceDeserializer;
import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.common.serializers.deserializers.cache.JSONObjectExtensions;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

// TODO: check if methods should be private
public class BacktraceDataDeserializer implements Deserializable<BacktraceData> {

    private final static FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceData.class);

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
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.symbolication)),
                obj.optLong(fieldNameLoader.get(Fields.timestamp)),
                obj.optString(fieldNameLoader.get(Fields.langVersion)),
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.agentVersion)),
                getAttributes(obj.optJSONObject(fieldNameLoader.get(Fields.attributes))),
                JSONObjectExtensions.optStringOrNull(obj, fieldNameLoader.get(Fields.mainThread)),
                getClassifiers(obj.optJSONArray(fieldNameLoader.get(Fields.classifiers))),
                getBacktraceReport(obj.optJSONObject(fieldNameLoader.get(Fields.report))),
                getAnnotations(obj.optJSONObject(fieldNameLoader.get(Fields.annotations))),
                getSourceCode(obj.optJSONObject(fieldNameLoader.get(Fields.sourceCode))),
                getThreadInformation(obj.optJSONObject(fieldNameLoader.get(Fields.threadInformationMap)))
        );
    }

    public BacktraceReport getBacktraceReport(JSONObject obj) throws JSONException {
        Deserializable<BacktraceReport> deserializer = BacktraceDeserializer.getDeserializer(BacktraceReport.class);
        return deserializer.deserialize(obj);
    }

    public Map<String, Object> getAnnotations(JSONObject obj) throws JSONException {
        // TODO: Fix: throwing exception
        return MapDeserializer.toMap(obj);
    }

    public Map<String, SourceCode> getSourceCode(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        SourceCodeDeserializer deserializer = new SourceCodeDeserializer();

        Map<String, SourceCode> result = new HashMap<>();

        Iterator<String> keys = obj.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (obj.get(key) instanceof JSONObject) {
                    result.put(key, deserializer.deserialize((JSONObject) obj.get(key)));
                }
            } catch (JSONException e) {
//                BacktraceLogger.e(LOG,)
                // TODO:
            }
        }
        return result;
    }

    public Map<String, ThreadInformation> getThreadInformation(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        Map<String, ThreadInformation> result = new HashMap<>();
        final ThreadInformationDeserializer deserializer = new ThreadInformationDeserializer();
        Iterator<String> keys = obj.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (obj.get(key) instanceof JSONObject) {
                    result.put(key, deserializer.deserialize((JSONObject) obj.get(key)));
                }
            } catch (JSONException e) {
                // TODO:
            }
        }
        return result;
    }

    public String[] getClassifiers(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }

        String[] result = new String[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object object = jsonArray.get(i);
                result[i] = object.toString();
            } catch (JSONException exception) {
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

        while (keys.hasNext()) {
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
