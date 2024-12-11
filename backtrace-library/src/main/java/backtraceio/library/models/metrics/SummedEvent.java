package backtraceio.library.models.metrics;

import java.util.Map;

import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.common.json.serialization.SerializedName;

public final class SummedEvent extends Event {

    @SerializedName("metric_group")
    private final String name;

    public SummedEvent(String name) {
        this(name, null);
    }

    public SummedEvent(String name, Map<String, String> attributes) {
        this(name, BacktraceTimeHelper.getTimestampSeconds(), attributes);
    }

    public SummedEvent(String name, long timestamp, Map<String, String> attributes) {
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

    public void addAttributes(Map<String, String> attributes) {
        addAttributesImpl(attributes);
    }
}
