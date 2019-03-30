package backtraceio.library.models.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Get report annotations - environment variables and application dependencies
 */
public class Annotations extends HashMap<String, Object>{

    public Annotations(Object exceptionMessage, Map<String, Object> complexAttributes)
    {
        this.put("Environment Variables", System.getenv());
        this.addComplexAttributes(complexAttributes);
        this.addExceptionDetails(exceptionMessage);
    }

    private void addComplexAttributes(Map<String, Object> complexAttributes)
    {
        if(complexAttributes != null) {
            this.putAll(complexAttributes);
        }
    }

    private void addExceptionDetails(final Object exceptionMessage){
        this.put("Exception", new  AnnotationException(exceptionMessage));
    }

    class AnnotationException{
        Object Message;

        AnnotationException(Object message){ setMessage(message); }

        void setMessage(Object message){ this.Message = message; }
    }
}
