package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.models.json.BacktraceAttributes;

public class SummedEventsPayload extends EventsPayload {
    @SerializedName("summed_events")
    private final ConcurrentLinkedDeque<Event> summedEvents;

    protected SummedEventsPayload(BacktraceAttributes backtraceAttributes, ConcurrentLinkedDeque<Event> events, int droppedEvents) {
        super(backtraceAttributes, droppedEvents);
        this.summedEvents = events;
    }

    public ConcurrentLinkedDeque<Event> getEvents() {
        return this.summedEvents;
    }
}
