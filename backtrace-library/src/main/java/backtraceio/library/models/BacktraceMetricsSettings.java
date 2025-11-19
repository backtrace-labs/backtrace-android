package backtraceio.library.models;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.services.BacktraceMetrics;

public class BacktraceMetricsSettings {
    private final String universe;
    private final String token;
    private final String baseUrl;
    private final long timeIntervalMillis;
    private final int timeBetweenRetriesMillis;

    /**
     * Backtrace metrics settings
     *
     * @param credentials Backtrace API credentials
     */
    public BacktraceMetricsSettings(BacktraceCredentials credentials) {
        this(
                credentials,
                BacktraceMetrics.defaultBaseUrl,
                BacktraceMetrics.defaultTimeIntervalMs,
                BacktraceMetrics.defaultTimeBetweenRetriesMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param credentials Backtrace API credentials
     * @param baseUrl     Base URL to send metrics
     */
    public BacktraceMetricsSettings(BacktraceCredentials credentials, String baseUrl) {
        this(
                credentials,
                baseUrl,
                BacktraceMetrics.defaultTimeIntervalMs,
                BacktraceMetrics.defaultTimeBetweenRetriesMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param credentials        Backtrace API credentials
     * @param timeIntervalMillis Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(BacktraceCredentials credentials, long timeIntervalMillis) {
        this(
                credentials,
                BacktraceMetrics.defaultBaseUrl,
                timeIntervalMillis,
                BacktraceMetrics.defaultTimeBetweenRetriesMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param credentials        Backtrace API credentials
     * @param baseUrl            Base URL to send metrics
     * @param timeIntervalMillis Time interval between metrics auto-send events, 0 disables auto-send
     */
    public BacktraceMetricsSettings(BacktraceCredentials credentials, String baseUrl, long timeIntervalMillis) {
        this(credentials, baseUrl, timeIntervalMillis, BacktraceMetrics.defaultTimeBetweenRetriesMs);
    }

    /**
     * Backtrace metrics settings
     *
     * @param credentials              Backtrace API credentials
     * @param baseUrl                  Base URL to send metrics
     * @param timeIntervalMillis       Time interval between metrics auto-send events, 0 disables auto-send
     * @param timeBetweenRetriesMillis Maximum time between retries in milliseconds
     */
    public BacktraceMetricsSettings(
            BacktraceCredentials credentials, String baseUrl, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.universe = credentials.getUniverseName();
        this.token = credentials.getSubmissionToken();
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

    public String getSubmissionUrl(String urlPrefix) {
        return this.getBaseUrl() + "/" + urlPrefix + "/submit?token=" + this.getToken() + "&universe="
                + this.getUniverseName();
    }

    public boolean isBacktraceServer() {
        return this.getUniverseName() != null;
    }
}
