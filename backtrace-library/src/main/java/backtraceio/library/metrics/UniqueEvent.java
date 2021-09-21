package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.logger.BacktraceLogger;

public class UniqueEvent extends Event {

    private final static transient String LOG_TAG = UniqueEvent.class.getSimpleName();

    @SerializedName("unique")
    private List<String> name;

    UniqueEvent(String name) {
        this(name, System.currentTimeMillis() / 1000, new HashMap<String, Object>());
    }

    UniqueEvent(String name, long timestamp, Map<String, Object> attributes) {
        super(timestamp);
        this.attributes = attributes;
        this.name = new ArrayList<String>() {{
            add(name);
        }};
    }

    // The spec specifies name as a JSON array but we don't change the interface yet
    @Override
    public String getName() {
        if (this.name != null && this.name.size() > 0) {
            return this.name.get(0);
        }
        BacktraceLogger.e(LOG_TAG, "Unique Event name must not be null or empty");
        return new String();
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
