package backtraceio.library.common.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import backtraceio.library.common.serializers.deserializers.BacktraceApiResultDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceDataDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceDatabaseRecordDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceReportDeserializer;
import backtraceio.library.common.serializers.deserializers.Deserializable;
import backtraceio.library.common.serializers.deserializers.ExceptionDeserializer;
import backtraceio.library.common.serializers.deserializers.ReflectionDeserializer;
import backtraceio.library.common.serializers.deserializers.ThreadInformationDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDeserializer {

    public final static ReflectionDeserializer DEFAULT_DESERIALIZER = new ReflectionDeserializer();
    public static HashMap<Class, Deserializable> deserializers = new HashMap<Class, Deserializable>() {{
        put(BacktraceApiResult.class, new BacktraceApiResultDeserializer());
        put(BacktraceReport.class, new BacktraceReportDeserializer());
        put(BacktraceData.class, new BacktraceDataDeserializer());
//        put(SourceCode.class, new SourceCodeDeserializer());
//        put(SourceCodeData.class, new SourceCodeDeserializer()),
        put(Exception.class, new ExceptionDeserializer());
        put(ThreadInformation.class, new ThreadInformationDeserializer());
        put(BacktraceDatabaseRecord.class, new BacktraceDatabaseRecordDeserializer());
    }};

    // TODO: Check if we need it?
//    public static void registerDeserializer(Class clazz, Deserializable obj) {
//        deserializers.put(clazz, obj);
//    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(JSONObject obj, Class<T> clazz) throws JSONException {
        if (obj == null) {
            return null;
        }
        if (deserializers.containsKey(clazz)) {
            Deserializable<T> deserializer = deserializers.get(clazz);
            if (deserializer == null) {
                return (T) DEFAULT_DESERIALIZER.deserialize(obj, clazz);
            }
            return (T) deserializer.deserialize(obj);
        }
        // todo: maybe return unsupported
        return (T) DEFAULT_DESERIALIZER.deserialize(obj, clazz);
    }
}
