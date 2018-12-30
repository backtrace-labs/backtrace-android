package backtraceio.library;

import android.util.Log;

public class BacktraceCredentials {
    private String TAG = "BACKTRACE.IO";
    private String endpointUrl;
    private String submissionToken;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getSubmissionToken() {
        return submissionToken;
    }

    public BacktraceCredentials(String endpointUrl, String submissionToken)
    {
        this.endpointUrl = endpointUrl;
        this.submissionToken = submissionToken;
        Log.d(TAG, endpointUrl);
    }
}
