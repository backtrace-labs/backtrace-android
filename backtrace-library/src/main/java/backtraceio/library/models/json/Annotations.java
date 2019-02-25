package backtraceio.library.models.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Get report annotations - environment variables and application dependencies
 */
public class Annotations extends HashMap<String, Object>{

    public Annotations()
    {
        this.put("Environment Variables", System.getenv());
    }

    public void addComplexAttributes(Map<String, Object> complexAttributes)
    {
        if(complexAttributes != null) {
            this.putAll(complexAttributes);
        }
    }
}
