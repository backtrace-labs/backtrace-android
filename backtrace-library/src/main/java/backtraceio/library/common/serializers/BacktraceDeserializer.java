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
import backtraceio.library.common.serializers.deserializers.SourceCodeDeserializer;
import backtraceio.library.common.serializers.deserializers.ThreadInformationDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDeserializer {
    public final static ReflectionDeserializer DEFAULT_DESERIALIZER = new ReflectionDeserializer();
    public static final HashMap<Class<?>, Deserializable<?>> deserializers = new HashMap<Class<?>, Deserializable<?>>() {{
        put(BacktraceApiResult.class, new BacktraceApiResultDeserializer());
        put(BacktraceReport.class, new BacktraceReportDeserializer());
        put(BacktraceData.class, new BacktraceDataDeserializer());
        put(SourceCode.class, new SourceCodeDeserializer());
        put(Exception.class, new ExceptionDeserializer());
        put(ThreadInformation.class, new ThreadInformationDeserializer());
        put(BacktraceDatabaseRecord.class, new BacktraceDatabaseRecordDeserializer());
    }};

    // TODO: Check if we need it?
//    public static void registerCustomDeserializer(Class clazz, Deserializable obj) {
//        deserializers.put(clazz, obj);
//    }
    @SuppressWarnings("unchecked")
    public static <T> Deserializable<T> getDeserializer(Class<T> clazz) {
        if (deserializers.containsKey(clazz)) {
            Deserializable<?> deserializer = deserializers.get(clazz);

            if (deserializer == null) {
                return null;
            }
            return (Deserializable<T>) deserializer;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(JSONObject obj, Class<T> clazz) throws JSONException {
        if (obj == null) {
            return null;
        }
        Deserializable<T> deserializer = BacktraceDeserializer.getDeserializer(clazz);

        if (deserializer != null) {
            return (T) deserializer.deserialize(obj);
        }

        return (T) DEFAULT_DESERIALIZER.deserialize(obj, clazz);
    }
}
