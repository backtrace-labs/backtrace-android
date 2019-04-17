package backtraceio.library.services;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.HttpException;


public class BacktraceHttpAsyncTask extends AsyncTask<Void, Void, BacktraceResult> {

    private static transient String LOG_TAG = BacktraceHttpAsyncTask.class.getSimpleName();

    /**
     * Data which will be send to Backtrace API saved in JSON format
     */
    private String json;

    /**
     * Request identifier
     */
    private UUID requestId;

    /**
     * Path to attachments which should be send to Backtrace API with request
     */
    private List<String> attachments;

    /**
     * Current BacktraceReport
     */
    private BacktraceReport report;

    /**
     * Server URL
     */
    private String url;

    /**
     * Event triggered on server response
     */
    private OnServerResponseEventListener onServerResponse;

    /**
     * Event triggered on server error
     */
    private OnServerErrorEventListener onServerError;

    /**
     * Event triggered after send request to Backtrace API
     */
    private OnAfterSendEventListener afterSend;

    public BacktraceHttpAsyncTask(String url, UUID requestId, String json, List<String>
            attachments, BacktraceReport report, OnServerResponseEventListener onServerResponse,
                                  OnServerErrorEventListener onServerError,
                                  OnAfterSendEventListener afterSend) {
        BacktraceLogger.d(LOG_TAG, "Initializing Http Async Task");
        this.requestId = requestId;
        this.json = json;
        this.attachments = attachments;
        this.report = report;
        this.url = url;
        this.onServerResponse = onServerResponse;
        this.onServerError = onServerError;
        this.afterSend = afterSend;
    }

    /**
     * Sending diagnostic data into Backtrace server API
     */
    @Override
    protected BacktraceResult doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result;

        try {
            URL url = new URL(this.url);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            urlConnection.setChunkedStreamingMode(128 * 1024);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");

            urlConnection.setRequestProperty("Content-Type",
                    MultiFormRequestHelper.getContentType());

            BacktraceLogger.d(LOG_TAG, "HttpURLConnection successfully initialized");
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());

            MultiFormRequestHelper.addJson(request, json);
            MultiFormRequestHelper.addFiles(request, attachments);
            MultiFormRequestHelper.addEndOfRequest(request);

            request.flush();
            request.close();

            int statusCode = urlConnection.getResponseCode();
            BacktraceLogger.d(LOG_TAG, "Received response status from Backtrace API for HTTP request is: " + Integer.toString(statusCode));

            if (statusCode == HttpURLConnection.HTTP_OK) {
                result = BacktraceSerializeHelper.backtraceResultFromJson(
                        getResponse(urlConnection)
                );
                result.setBacktraceReport(report);
                if (this.onServerResponse != null) {
                    BacktraceLogger.d(LOG_TAG, "Custom server response handler for response from Backtrace API");
                    this.onServerResponse.onEvent(result);
                }
            } else {
                String message = getResponse(urlConnection);
                message = (message == null || message.equals("")) ?
                        urlConnection.getResponseMessage() : message;
                throw new HttpException(statusCode, String.format("%s: %s",
                        Integer.toString(statusCode), message));
            }

        } catch (Exception e) {
            if (this.onServerError != null) {
                BacktraceLogger.d(LOG_TAG, "Custom handler on server error");
                this.onServerError.onEvent(e);
            }
            BacktraceLogger.e(LOG_TAG, "Sending HTTP request failed to Backtrace API", e);
            return BacktraceResult.OnError(report, e);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                    BacktraceLogger.d(LOG_TAG, "Disconnecting HttpUrlConnection successful");
                } catch (Exception e) {
                    BacktraceLogger.e(LOG_TAG, "Disconnecting HttpUrlConnection failed", e);
                    return BacktraceResult.OnError(report, e);
                }
            }
        }
        return result;
    }

    @Override
    public void onPostExecute(BacktraceResult result) {
        if (afterSend != null) {
            BacktraceLogger.d(LOG_TAG, "Custom handler after send report to Backtrace API");
            afterSend.onEvent(result);
        }
        super.onPostExecute(result);
    }

    /**
     * Read response message from HTTP response
     *
     * @param urlConnection current HTTP connection
     * @return response from HTTP request
     * @throws IOException
     */
    private String getResponse(HttpURLConnection urlConnection) throws IOException {
        BacktraceLogger.d(LOG_TAG, "Reading response from HTTP request");

        InputStream inputStream;
        if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = urlConnection.getInputStream();
        } else {
            inputStream = urlConnection.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                inputStream));

        StringBuilder responseSB = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            responseSB.append(line);
        }
        br.close();
        return responseSB.toString();
    }
}