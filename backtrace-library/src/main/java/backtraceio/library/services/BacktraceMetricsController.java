package backtraceio.library.services;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.common.UnsupportedMetricsServer;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.UniqueEvent;

public class BacktraceMetricsController {

    private final BacktraceMetrics metrics;
    public BacktraceMetricsController(Context context, Map<String, Object> attributes, Api backtraceApi, BacktraceCredentials credentials) {
        this.metrics = credentials.isBacktraceServerUrl() ? new BacktraceMetrics(context, attributes, backtraceApi, credentials) : null;
    }

    public BacktraceMetrics getMetrics() throws UnsupportedMetricsServer {
        if (metrics != null) {
            return metrics;
        }

        throw new UnsupportedMetricsServer(); // TODO: use dedicated exception type
    }
}
