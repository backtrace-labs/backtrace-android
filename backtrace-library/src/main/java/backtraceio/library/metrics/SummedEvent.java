package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.base.BacktraceBase;

public final class SummedEvent extends Event {

    @SerializedName("metric_group")
    private final String name;

    SummedEvent(String name) {
        this(name, new HashMap<String, Object>());
    }

    SummedEvent(String name, Map<String, Object> attributes) {
        this(name, BacktraceBase.getTimestampSeconds(), attributes);
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
        addAttributesImpl(attributes);
    }
}
