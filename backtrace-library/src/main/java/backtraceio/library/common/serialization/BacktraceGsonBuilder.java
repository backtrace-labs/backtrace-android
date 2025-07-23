package backtraceio.library.common.serialization;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BacktraceGsonBuilder implements CustomGsonBuilder {

    @Override
    public Gson buildGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
//                .registerTypeHierarchyAdapter(Throwable.class, new ThrowableAdapter())
                .registerTypeAdapterFactory(new ThrowableTypeAdapterFactory2())
                .create();
    }
}
