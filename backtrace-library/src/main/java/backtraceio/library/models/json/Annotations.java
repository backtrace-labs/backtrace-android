package backtraceio.library.models.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Get report annotations - environment variables and application dependencies
 */
public class Annotations {

    public static Map<String, Object> getAnnotations(Object exceptionMessage, Map<String, Object> complexAttributes) {
        Map<String, Object> result = new HashMap<>();
        result.put("Environment Variables", System.getenv());
        if (complexAttributes != null) {
            result.putAll(complexAttributes);
        }

        result.put("Exception", new AnnotationException(exceptionMessage));
        return result;
    }
}

class AnnotationException {
    private Object message;

    AnnotationException(Object message) {
        setMessage(message);
    }

    void setMessage(Object message) {
        this.message = message;
    }
}