package backtraceio.library.models.metrics;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.json.serialization.SerializedName;

public abstract class Event {

    @SerializedName("timestamp")
    protected long timestamp;

    /**
     * The event should always have attributes. Event without attributes won't be processed
     */
    @SerializedName("attributes")
    protected Map<String, String> attributes = new HashMap<>();

    public Event(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public abstract String getName();

    protected void addAttributesImpl(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        this.attributes.putAll(attributes);
    }
}
