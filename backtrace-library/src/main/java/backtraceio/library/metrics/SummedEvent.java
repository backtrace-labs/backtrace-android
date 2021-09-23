package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public final class SummedEvent extends Event {

    @SerializedName("metric_group")
    private final String name;

    SummedEvent(String name) {
        this(name, System.currentTimeMillis() / 1000, new HashMap<String, Object>());
    }

    SummedEvent(String name, long timestamp, Map<String, Object> attributes) {
        super(timestamp);
        this.name = name;
        this.attributes = attributes;
    }

    SummedEvent(SummedEvent summedEvent) {
        this(summedEvent.name, summedEvent.timestamp, summedEvent.attributes);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void addAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }
}
