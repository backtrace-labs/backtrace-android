package backtraceio.library.models.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.logger.BacktraceLogger;

public class UniqueEvent extends Event {

    private final static transient String LOG_TAG = UniqueEvent.class.getSimpleName();

    /**
     * Unique events API spec requires unique events to be a JSON array, but we still treat it as a single string
     */
    @SerializedName("unique")
    private final List<String> name;

    public UniqueEvent(String name) {
        this(name, new HashMap<String, Object>());
    }

    public UniqueEvent(String name, Map<String, Object> attributes) {
        this(name, BacktraceTimeHelper.getTimestampSeconds(), attributes);
    }

    public UniqueEvent(String name, long timestamp, Map<String, Object> attributes) {
        super(timestamp);
        this.name = new ArrayList<String>() {{
            add(name);
        }};
        addAttributesImpl(attributes);
    }

    /**
     * Unique events API spec requires unique events to be a JSON array, but we still treat it as a single string
     */
    @Override
    public String getName() {
        if (this.name != null && this.name.size() > 0 && !BacktraceStringHelper.isNullOrEmpty(this.name.get(0))) {
            return this.name.get(0);
        }
        BacktraceLogger.e(LOG_TAG, "Unique Event name must not be null or empty");
        return "";
    }

    public void update(long timestamp, Map<String, Object> attributes) {
        this.timestamp = timestamp;
        addAttributesImpl(attributes);
    }
}
