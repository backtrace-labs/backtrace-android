package backtraceio.library;

/**
 * Backtrace credentials information
 */
public class BacktraceCredentials {
    private String endpointUrl;
    private String submissionToken;

    /**
     * Initialize Backtrace credentials
     *
     * @param endpointUrl
     * @param submissionToken
     */
    public BacktraceCredentials(String endpointUrl, String submissionToken) {
        this.endpointUrl = endpointUrl;
        this.submissionToken = submissionToken;
    }

    /**
     * Get URL to Backtrace server API
     *
     * @return endpoint url
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }

    /**
     * Get an access token to Backtrace server API
     *
     * @return access token
     */
    public String getSubmissionToken() {
        return submissionToken;
    }
}
