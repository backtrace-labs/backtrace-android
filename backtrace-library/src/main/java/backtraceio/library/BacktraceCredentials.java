package backtraceio.library;

import android.net.Uri;

import java.net.URI;

/**
 * Backtrace credentials information
 */
public class BacktraceCredentials {
    /**
     * Data format
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String format = "json";

    private String endpointUrl;
    private String submissionToken;
    private Uri backtraceHostUri;

    /**
     * Initialize Backtrace credentials
     *
     * @param endpointUrl     endpoint url address
     * @param submissionToken server access token
     */
    public BacktraceCredentials(String endpointUrl, String submissionToken) {
        this.endpointUrl = endpointUrl;
        this.submissionToken = submissionToken;
    }

    public BacktraceCredentials(String backtraceHostUri) {
        this(Uri.parse(backtraceHostUri));
    }

    public BacktraceCredentials(Uri backtraceHostUri) {
        this.backtraceHostUri = backtraceHostUri;
    }

    /**
     * Get URL to Backtrace server API
     *
     * @return endpoint url
     */
    private String getEndpointUrl() {
        return endpointUrl;
    }

    /**
     * Get an access token to Backtrace server API
     *
     * @return access token
     */
    private String getSubmissionToken() {
        return submissionToken;
    }

    private Uri getBacktraceHostUri() {
        return backtraceHostUri;
    }

    private Uri getServerUrl() {
        String url = String.format("%spost?format=%s&token=%s", this.getEndpointUrl(),
                this.format, this.getSubmissionToken());
        return Uri.parse(url);
    }

    /**
     * Get submission URL to Backtrace API
     *
     * @return URL to Backtrace API
     */
    public Uri getSubmissionUrl() {
        Uri backtraceUri = getBacktraceHostUri();
        if (backtraceUri != null) {
            return backtraceUri;
        }
        return getServerUrl();
    }

    public Uri getMinidumpSubmissionUrl() {
        Uri backtraceJsonUri = getSubmissionUrl();
        String jsonUrl = backtraceJsonUri.toString();
        if (jsonUrl.contains("format=json")) {
            jsonUrl = jsonUrl.replace("format=json", "format=minidump");
        } else if (jsonUrl.contains(("/json"))) {
            jsonUrl = jsonUrl.replace("/json", "/minidump");
        } else {
            return null;
        }
        return Uri.parse(jsonUrl);
    }
}