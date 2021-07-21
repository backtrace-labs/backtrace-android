package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class UniqueEvent extends Event {

    @SerializedName("unique")
    private String name;

    UniqueEvent(String name) {
        this(name, System.currentTimeMillis() / 1000, new HashMap<String, Object>());
    }

    UniqueEvent(String name, long timestamp, Map<String, Object> attributes) {
        super(timestamp);
        this.attributes = attributes;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    void update(long timestamp, Map<String, Object> attributes) {
        this.timestamp = timestamp;
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                Object value = attributes.get(key);
                if (value != null && !value.toString().trim().isEmpty()) {
                       this.attributes.put(key, value);
                }
            }
        }
    }
}
