package backtraceio.library;

import android.net.Uri;

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

    private Uri getBacktraceHostUri() {
        return backtraceHostUri;
    }

    private Uri getServerUrl() {
        String serverUrl = this.getEndpointUrl();
        String prefix = serverUrl.endsWith("/") ? "" : "/";
        String url = String.format("%s%spost?format=%s&token=%s", serverUrl, prefix,
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

    // Using algorithm from backtrace-unity:
    // https://github.com/backtrace-labs/backtrace-unity/blob/553aab2b39c318ff96ebed4bc739bf2c87304649/Runtime/Model/BacktraceConfiguration.cs#L290
    public String getUniverseName() {
        String submissionUrl = getSubmissionUrl().toString();
        final String backtraceSubmitUrl = "https://submit.backtrace.io/";
        if (submissionUrl.startsWith(backtraceSubmitUrl)) {
            int universeIndexStart = backtraceSubmitUrl.length();
            int universeIndexEnd = submissionUrl.indexOf('/', universeIndexStart);
            if (universeIndexEnd == -1) {
                throw new IllegalArgumentException("Invalid Backtrace URL");
            }
            return submissionUrl.substring(universeIndexStart, universeIndexEnd);
        } else {
            final String backtraceDomain = "backtrace.io";
            if (submissionUrl.indexOf(backtraceDomain) == -1) {
                throw new IllegalArgumentException("Invalid Backtrace URL");
            }

            Uri uri = Uri.parse(submissionUrl);
            return uri.getHost().substring(0, uri.getHost().indexOf("."));
        }
    }

    /**
     * Get an access token to Backtrace server API
     *
     * @return: access token
     * @note: Using algorithm from backtrace-unity https://github.com/backtrace-labs/backtrace-unity/blob/553aab2b39c318ff96ebed4bc739bf2c87304649/Runtime/Model/BacktraceConfiguration.cs#L320
     */
    public String getSubmissionToken() {
        if (submissionToken != null)
            return submissionToken;

        final int tokenLength = 64;
        final String tokenQueryParam = "token=";
        String submissionUrl = getSubmissionUrl().toString();
        final int tokenEndIndex = submissionUrl.lastIndexOf("/");
        if (submissionUrl.contains("submit.backtrace.io")) {
            if (tokenEndIndex - tokenLength < 0) {
                return null;
            }
            return submissionUrl.substring(tokenEndIndex - tokenLength, tokenEndIndex);
        }
        final int tokenQueryParamStartIndex = submissionUrl.indexOf(tokenQueryParam);
        if (tokenQueryParamStartIndex == -1) {
            return null;
        }

        final int tokenParamStartIndex = tokenQueryParamStartIndex + tokenQueryParam.length();
        return submissionUrl.substring(tokenParamStartIndex, tokenParamStartIndex + tokenLength);
    }
}