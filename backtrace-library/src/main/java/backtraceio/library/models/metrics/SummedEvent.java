package backtraceio.library.models.metrics;

import backtraceio.library.common.serializers.SerializedName;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.BacktraceTimeHelper;

public final class SummedEvent extends Event {

    @SerializedName("metric_group")
    private final String name;

    public SummedEvent(String name) {
        this(name, new HashMap<String, Object>());
    }

    public SummedEvent(String name, Map<String, Object> attributes) {
        this(name, BacktraceTimeHelper.getTimestampSeconds(), attributes);
    }

    public SummedEvent(String name, long timestamp, Map<String, Object> attributes) {
        super(timestamp);
        this.name = name;
        addAttributesImpl(attributes);
    }

    public SummedEvent(SummedEvent summedEvent) {
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
