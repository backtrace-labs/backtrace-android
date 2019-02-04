package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Get report annotations - environment variables and application dependencies
 */
public class Annotations {

    @SerializedName("ComplexAttributes ")
    public Map<String, Object> complexAttributes;

    @SerializedName("Environment Variables")
    public Map<String, String> environmentVariables;

    @SerializedName("Dependencies")
    public Map<String, Object> dependencies = new HashMap<>();

    public Annotations(Map<String, Object> complexAttributes)
    {
        this.complexAttributes = complexAttributes;

        this.environmentVariables = System.getenv();
    }
}
