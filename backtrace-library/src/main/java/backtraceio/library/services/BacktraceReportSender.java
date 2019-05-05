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
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.HttpException;

public class BacktraceReportSender {

    private static final String LOG_TAG = BacktraceReportSender.class.getSimpleName();


    public static BacktraceResult sendReport(String serverUrl, String json, List<String> attachments, BacktraceReport report, OnServerErrorEventListener errorCallback) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result;

        try {
            URL url = new URL(serverUrl);
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
            } else {
                String message = getResponse(urlConnection);
                message = (message == null || message.equals("")) ?
                        urlConnection.getResponseMessage() : message;
                throw new HttpException(statusCode, String.format("%s: %s",
                        Integer.toString(statusCode), message));
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
