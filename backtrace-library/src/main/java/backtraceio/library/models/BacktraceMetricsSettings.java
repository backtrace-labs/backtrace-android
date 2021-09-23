package backtraceio.library.models;

import backtraceio.library.metrics.BacktraceMetrics;

public class BacktraceMetricsSettings {
    private final String universe;
    private final String token;
    private final String baseUrl;
    private final long timeIntervalMillis;
    private final int timeBetweenRetriesMillis;

    /**
     * Backtrace metrics settings
     *
     * @param universeName Backtrace universe name
     * @param token        Backtrace submission token
     */
    public BacktraceMetricsSettings(String universeName, String token) {
        this(universeName, token, BacktraceMetrics.defaultBaseUrl);
    }

    /**
     * Backtrace metrics settings
     *
     * @param universeName Backtrace universe name
     * @param token        Backtrace submission token
     * @param baseUrl      Base URL to send metrics
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl) {
        this(universeName, token, baseUrl, BacktraceMetrics.defaultTimeIntervalMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param universeName       Backtrace universe name
     * @param token              Backtrace submission token
     * @param timeIntervalMillis Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(String universeName, String token, long timeIntervalMillis) {
        this(universeName, token, BacktraceMetrics.defaultBaseUrl, timeIntervalMillis);
    }

    /**
     * Backtrace metrics settings
     *
     * @param universeName       Backtrace universe name
     * @param token              Backtrace submission token
     * @param baseUrl            Base URL to send metrics
     * @param timeIntervalMillis Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl, long timeIntervalMillis) {
        this(universeName, token, baseUrl, timeIntervalMillis, BacktraceMetrics.defaultTimeBetweenRetriesMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param universeName             Backtrace universe name
     * @param token                    Backtrace submission token
     * @param baseUrl                  Base URL to send metrics
     * @param timeIntervalMillis       Time interval between metrics auto-send events, 0 disables auto-send
     * @param timeBetweenRetriesMillis Maximum time between retries in milliseconds
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.universe = universeName;
        this.token = token;
        this.baseUrl = baseUrl;
        this.timeIntervalMillis = timeIntervalMillis;
        this.timeBetweenRetriesMillis = timeBetweenRetriesMillis;
    }

    public String getUniverseName() {
        return universe;
    }

    public String getToken() {
        return token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public long getTimeIntervalMillis() {
        return timeIntervalMillis;
    }

    public int getTimeBetweenRetriesMillis() {
        return timeBetweenRetriesMillis;
    }
}
