package backtraceio.library.common.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import backtraceio.library.common.serializers.deserializers.BacktraceDataDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceDatabaseRecordDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceReportDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceApiResultDeserializer;
import backtraceio.library.common.serializers.deserializers.Deserializable;
import backtraceio.library.common.serializers.deserializers.ExceptionDeserializer;
import backtraceio.library.common.serializers.deserializers.ReflectionDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceDeserializer {

    public final static Deserializable<Object> DEFAULT_DESERIALIZER = new ReflectionDeserializer();
    public static HashMap<Class, Deserializable> deserializers = new HashMap<Class, Deserializable>() {{
        put(BacktraceApiResult.class, new BacktraceApiResultDeserializer());
        put(BacktraceReport.class, new BacktraceReportDeserializer());
        put(BacktraceData.class, new BacktraceDataDeserializer());
        put(Exception.class, new ExceptionDeserializer());
        put(BacktraceDatabaseRecord.class, new BacktraceDatabaseRecordDeserializer());
    }};

    public static void registerDeserializer(Class clazz, Deserializable obj) {
        deserializers.put(clazz, obj);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(JSONObject obj, Class<T> clazz) throws JSONException {
        if (obj == null) {
//            LOGG
            // TODO: Add logging
            return null;
        }
        if (deserializers.containsKey(clazz)) {
            // TODO: check if deserializers.get(clazz) is not null
            return (T) deserializers.get(clazz).deserialize(obj);
        }
        // todo: maybe return unsupported
        return (T) DEFAULT_DESERIALIZER.deserialize(obj);
    }
}