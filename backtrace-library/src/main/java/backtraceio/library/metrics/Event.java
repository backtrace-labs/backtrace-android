package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public abstract class Event {

    @SerializedName("timestamp")
    protected long timestamp;

    @SerializedName("attributes")
    protected Map<String, Object> attributes;

    public long getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    public abstract String getName();

    public Event(long timestamp) {
        this.timestamp = timestamp;
    }
}
