package backtraceio.library.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.common.RequestHelper;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.EventsResult;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.models.types.HttpException;

/**
 * Class for sending and processing HTTP request
 */
class BacktraceReportSender {

    private static final String LOG_TAG = BacktraceReportSender.class.getSimpleName();

    private static final int CHUNK_SIZE = 128 * 1024;

    /**
     * Send HTTP request for certain url server with information about device, error, attachments
     *
     * @param serverUrl     server http address to which the request will be sent
     * @param json          message with information about device and error
     * @param attachments   list of paths to files that should be sent
     * @param report        information about error
     * @param errorCallback event that will be executed after receiving an error from the server
     * @return information from the server about the result of processing the request
     */
    static BacktraceResult sendReport(String serverUrl, String json, List<String> attachments, BacktraceReport report, OnServerErrorEventListener errorCallback) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result;

        try {
            URL url = new URL(serverUrl);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            urlConnection.setChunkedStreamingMode(CHUNK_SIZE);
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
            BacktraceLogger.d(LOG_TAG, "Received response status from Backtrace API for HTTP request is: " + statusCode);

            if (statusCode == HttpURLConnection.HTTP_OK) {
                result = BacktraceSerializeHelper.backtraceResultFromJson(
                        getResponse(urlConnection)
                );
                result.setBacktraceReport(report);
            } else {
                String message = getResponse(urlConnection);
                message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                        urlConnection.getResponseMessage() : message;
                throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
            }

        } catch (Exception e) {
            if (errorCallback != null) {
                BacktraceLogger.d(LOG_TAG, "Custom handler on server error");
                errorCallback.onEvent(e);
            }
            BacktraceLogger.e(LOG_TAG, "Sending HTTP request failed to Backtrace API", e);
            result = BacktraceResult.OnError(report, e);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                    BacktraceLogger.d(LOG_TAG, "Disconnecting HttpUrlConnection successful");
                } catch (Exception e) {
                    BacktraceLogger.e(LOG_TAG, "Disconnecting HttpUrlConnection failed", e);
                    result = BacktraceResult.OnError(report, e);
                }
            }
        }
        return result;
    }

    /**
     * Send HTTP request for certain url server with information about events
     *
     * @param serverUrl     server http address to which the request will be sent
     * @param json          message wih information about events
     * @param payload       information about events
     * @param errorCallback event that will be executed after receiving an error from the server
     * @return information from the server about the result of processing the request
     */
    public static EventsResult sendEvents(String serverUrl, String json, EventsPayload payload, OnServerErrorEventListener errorCallback) {
        HttpURLConnection urlConnection = null;
        EventsResult result;
        int statusCode = -1;

        try {
            URL url = new URL(serverUrl);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Content-Type", RequestHelper.getContentType());

            BacktraceLogger.d(LOG_TAG, "HttpURLConnection successfully initialized");
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());

            RequestHelper.addJson(request, json);
            RequestHelper.addEndOfRequest(request);

            request.flush();
            request.close();

            statusCode = urlConnection.getResponseCode();
            BacktraceLogger.d(LOG_TAG, "Received response status from Backtrace API for HTTP request is: " + statusCode);

            if (statusCode == HttpURLConnection.HTTP_OK) {
                result = new EventsResult(payload, urlConnection.getResponseMessage(), BacktraceResultStatus.Ok, statusCode);
            } else {
                String message = getResponse(urlConnection);
                message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                        urlConnection.getResponseMessage() : message;
                throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
            }
        } catch (Exception e) {
            if (errorCallback != null) {
                BacktraceLogger.d(LOG_TAG, "Custom handler on server error");
                errorCallback.onEvent(e);
            }
            BacktraceLogger.e(LOG_TAG, "Sending HTTP request failed to Backtrace API", e);
            BacktraceLogger.e(LOG_TAG, "Failed HTTP request URL " + serverUrl);
            result = EventsResult.OnError(payload, e, statusCode);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                    BacktraceLogger.d(LOG_TAG, "Disconnecting HttpUrlConnection successful");
                } catch (Exception e) {
                    BacktraceLogger.e(LOG_TAG, "Disconnecting HttpUrlConnection failed", e);
                    result = EventsResult.OnError(payload, e, statusCode);
                }
            }
        }
        return result;
    }

    /**
     * Read response message from HTTP response
     *
     * @param urlConnection current HTTP connection
     * @return response from HTTP request
     * @throws IOException
     */
    private static String getResponse(HttpURLConnection urlConnection) throws IOException {
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
