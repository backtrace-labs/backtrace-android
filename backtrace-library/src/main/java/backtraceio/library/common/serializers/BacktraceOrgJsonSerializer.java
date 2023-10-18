package backtraceio.library.common.serializers;

import org.json.JSONException;

import backtraceio.library.common.serializers.BacktraceDataSerializer;
import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.common.serializers.naming.NamingPolicy;
import backtraceio.library.models.BacktraceData;

public class BacktraceOrgJsonSerializer {

    public static String toJson(Object object) { // TODO: remove IllegalAccessException
        if (object == null) {
            return null;
        }

        try {
            NamingPolicy namingPolicy = new NamingPolicy();
            Class clazz = object.getClass();

            if (clazz.equals(BacktraceData.class)) {
                BacktraceDataSerializer dataSerializer = new BacktraceDataSerializer(namingPolicy);
                return dataSerializer.toJson((BacktraceData) object).toString();
            }

            return SerializerHelper.serialize(namingPolicy, object).toString();
        }
        catch (Exception ex) { // TODO: improve error handling
            ex.printStackTrace();
            return null;
        }
    }
}
