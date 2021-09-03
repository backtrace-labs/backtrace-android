package backtraceio.library.metrics;

import android.content.pm.PackageManager;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceAttributes;

public abstract class EventsPayload {

    private static transient String LOG_TAG = EventsPayload.class.getSimpleName();

    @SerializedName("application")
    private final String application;

    @SerializedName("appversion")
    private final String appVersion;

    @SerializedName("dropped_events")
    private int droppedEvents;

    public EventsPayload(BacktraceAttributes backtraceAttributes, int droppedEvents) {
        String appVersion;
        this.application = backtraceAttributes.getApplicationName();
        try {
            appVersion = backtraceAttributes.getApplicationVersion();
        } catch (PackageManager.NameNotFoundException e) {
            BacktraceLogger.e(LOG_TAG, "Could not resolve application version");
            appVersion = "";
        }
        this.appVersion = appVersion;
        this.droppedEvents = droppedEvents;
    }

    public void setDroppedEvents(int droppedEvents) {
        this.droppedEvents = droppedEvents;
    }

    public int getDroppedEvents() { return this.droppedEvents; }

    public abstract ConcurrentLinkedDeque<Event> getEvents();
}
