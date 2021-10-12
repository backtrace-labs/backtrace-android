package backtraceio.library.services;

import android.content.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.models.metrics.UniqueEventsPayload;

public class UniqueEventsHandler extends BacktraceEventsHandler<UniqueEvent> {

    private final static transient String LOG_TAG = UniqueEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "unique-events";

    public UniqueEventsHandler(Context context, Map<String, Object> customAttributes,
                               Api api, final BacktraceHandlerThread backtraceHandlerThread, BacktraceMetricsSettings settings) {
        super(context, customAttributes, api, backtraceHandlerThread, urlPrefix, settings);
    }

    @Override
    public void sendStartupEvent(String eventName) {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        Map<String, Object> attributes = backtraceAttributes.getAllAttributes();

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        Object value = attributes.get(eventName);
        if (value == null) {
            BacktraceLogger.e(LOG_TAG, "Startup unique event does not exist within attributes, this should not happen");
        }

        if (BacktraceStringHelper.isObjectNotNullOrNotEmptyString(value)) {
            events.addLast(new UniqueEvent(eventName, attributes));
            send();
        } else {
            BacktraceLogger.e(LOG_TAG, "Startup unique event is empty, this should not happen");
        }
    }

    @Override
    protected UniqueEventsPayload getEventsPayload() {
        Map<String, Object> attributes = getAttributes();

        for (UniqueEvent event : events) {
            event.update(BacktraceTimeHelper.getTimestampSeconds(), attributes);
        }

        UniqueEventsPayload payload = new UniqueEventsPayload(events, application, appVersion);
        return payload;
    }

    @Override
    protected void sendEvents(ConcurrentLinkedDeque<UniqueEvent> events) {
        UniqueEventsPayload payload = getEventsPayload();
        api.sendEventsPayload(payload);
    }

    @Override
    protected void sendEventsPayload(EventsPayload<UniqueEvent> payload) {
        api.sendEventsPayload((UniqueEventsPayload) payload);
    }
}
