package backtraceio.library.metrics;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.services.BacktraceHandlerThread;

public class UniqueEventsHandler extends BacktraceEventsHandler {

    private final static transient String LOG_TAG = UniqueEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "unique-events";

    public UniqueEventsHandler(Context context, Map<String, Object> customAttributes,
                               Api api, final BacktraceHandlerThread backtraceHandlerThread, BacktraceMetricsSettings settings) {
        super(context, customAttributes, api, backtraceHandlerThread, urlPrefix, settings);
    }

    @Override
    public void sendStartupEvent(String eventName) {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        Map<String, Object> attributes = backtraceAttributes.getAllBacktraceAttributes();

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        Object value = attributes.get(eventName);
        if (value != null && !value.toString().trim().isEmpty()) {
            events.addLast(new UniqueEvent(eventName, System.currentTimeMillis() / 1000, attributes));
        }
        send();
    }

    @Override
    protected EventsPayload getEventsPayload() {
        Map<String, Object> attributes = new HashMap<String, Object>();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        attributes.putAll(backtraceAttributes.getAllBacktraceAttributes());

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        for (Event event : events) {
            if (!(event instanceof UniqueEvent)) {
                BacktraceLogger.e(LOG_TAG, "Cannot convert stored event to UniqueEvent");
                continue;
            }

            ((UniqueEvent) event).update(System.currentTimeMillis() / 1000,
                    attributes);
        }

        UniqueEventsPayload payload = new UniqueEventsPayload(backtraceAttributes, events, numRetries);
        return payload;
    }
}
