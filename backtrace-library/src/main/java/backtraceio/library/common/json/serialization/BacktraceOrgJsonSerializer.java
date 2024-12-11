package backtraceio.library.common.json.serialization;

import org.json.JSONException;

import backtraceio.library.common.json.naming.NamingPolicy;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;

public class BacktraceOrgJsonSerializer {
    private final static String LOG_TAG = BacktraceOrgJsonSerializer.class.getSimpleName();
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            final NamingPolicy namingPolicy = new NamingPolicy();
            final Class<?> clazz = object.getClass();

            if (clazz.equals(BacktraceData.class)) {
                BacktraceDataSerializer dataSerializer = new BacktraceDataSerializer(namingPolicy);
                return dataSerializer.toJson((BacktraceData) object).toString();
            }

            return SerializerHelper.serialize(namingPolicy, object).toString();
        }
        catch (JSONException jsonException) {
            BacktraceLogger.e(LOG_TAG, String.format("Can not serialize object %s", object), jsonException);
            return null;
        }
        catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, String.format("Exception during serialization of object %s", object), ex);
            return null;
        }
    }
}
