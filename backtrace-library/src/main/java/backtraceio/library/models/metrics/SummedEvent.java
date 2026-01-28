package backtraceio.library.models.metrics;

import backtraceio.gson.annotations.SerializedName;
import backtraceio.library.common.BacktraceTimeHelper;
import java.util.Map;

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
