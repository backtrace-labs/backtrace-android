package backtraceio.library.models.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.BacktraceStringHelper;

public abstract class Event {

    @SerializedName("timestamp")
    protected long timestamp;

    @SerializedName("attributes")
    protected Map<String, Object> attributes;

    public Event(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    public abstract String getName();

    protected void addAttributesImpl(Map<String, Object> attributes) {
        if (attributes == null || attributes.size() == 0) {
            return;
        }

        Map<String, Object> attributesNoEmpty = new HashMap<String, Object>();

        for (String key : attributes.keySet()) {
            Object value = attributes.get(key);
            if (BacktraceStringHelper.isObjectNotNullOrNotEmptyString(value)) {
                attributesNoEmpty.put(key, value);
            }
        }

        if (this.attributes == null) {
            this.attributes = attributesNoEmpty;
        } else {
            this.attributes.putAll(attributesNoEmpty);
        }
    }
}
