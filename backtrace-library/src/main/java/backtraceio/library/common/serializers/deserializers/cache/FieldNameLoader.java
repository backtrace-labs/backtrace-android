package backtraceio.library.common.serializers.deserializers.cache;

public class FieldNameLoader {
    private final Class clazz;

    public FieldNameLoader(Class clazz) {
        this.clazz = clazz;
    }

    public String get(String fieldName) {
        return FieldNameCache.getAnnotation(clazz, fieldName);
    }
}
