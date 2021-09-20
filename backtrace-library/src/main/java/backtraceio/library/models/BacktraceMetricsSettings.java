package backtraceio.library.models;

import backtraceio.library.metrics.BacktraceMetrics;

public class BacktraceMetricsSettings {
    /**
     * Backtrace metrics settings
     * @param universeName              Backtrace universe name
     * @param token                     Backtrace submission token
     */
    public BacktraceMetricsSettings(String universeName, String token) {
        this(universeName, token, BacktraceMetrics.defaultBaseUrl);
    }

    /**
     * Backtrace metrics settings
     * @param universeName              Backtrace universe name
     * @param token                     Backtrace submission token
     * @param baseUrl                   Base URL to send metrics
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl) {
        this(universeName, token, baseUrl, BacktraceMetrics.defaultTimeIntervalMillis);
    }

    /**
     * Backtrace metrics settings
     * @param universeName              Backtrace universe name
     * @param token                     Backtrace submission token
     * @param timeIntervalMillis        Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(String universeName, String token, long timeIntervalMillis) {
        this(universeName, token, BacktraceMetrics.defaultBaseUrl, timeIntervalMillis);
    }

    /**
     * Backtrace metrics settings
     * @param universeName              Backtrace universe name
     * @param token                     Backtrace submission token
     * @param baseUrl                   Base URL to send metrics
     * @param timeIntervalMillis        Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl, long timeIntervalMillis) {
        this(universeName, token, baseUrl, timeIntervalMillis, BacktraceMetrics.defaultTimeBetweenRetriesMillis);
    }

    /**
     * Backtrace metrics settings
     * @param universeName              Backtrace universe name
     * @param token                     Backtrace submission token
     * @param baseUrl                   Base URL to send metrics
     * @param timeIntervalMillis        Time interval between metrics auto-send events, 0 disables auto-send
     * @param timeBetweenRetriesMillis  Maximum time between retries in milliseconds
     */
    public BacktraceMetricsSettings(String universeName, String token, String baseUrl, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.universe = universeName;
        this.token = token;
        this.baseUrl = baseUrl;
        this.timeIntervalMillis = timeIntervalMillis;
        this.timeBetweenRetriesMillis = timeBetweenRetriesMillis;
    }

    private String universe;
    public String getUniverseName() { return universe; }

    private String token;
    public String getToken() { return token; }

    private String baseUrl;
    public String getBaseUrl() { return baseUrl; }

    private long timeIntervalMillis;
    public long getTimeIntervalMillis() { return timeIntervalMillis; }

    private int timeBetweenRetriesMillis;
    public int getTimeBetweenRetriesMillis() { return timeBetweenRetriesMillis; }
}
