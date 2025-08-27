package backtraceio.library.models.metrics;

import backtraceio.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

public class SummedEventsPayload extends EventsPayload<SummedEvent> {
    @SerializedName("summed_events")
    private final ConcurrentLinkedDeque<SummedEvent> summedEvents;

    public SummedEventsPayload(ConcurrentLinkedDeque<SummedEvent> events, String application, String appVersion) {
        super(application, appVersion);
        this.summedEvents = events;
    }

    @Override
    public ConcurrentLinkedDeque<SummedEvent> getEvents() {
        return this.summedEvents;
    }
}
