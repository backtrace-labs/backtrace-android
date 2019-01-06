package backtraceio.library.services;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.HttpException;


public class BacktraceHttpAsyncTask extends AsyncTask<Void, Void, BacktraceResult> {
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
    private ArrayList<String> attachments;
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

    public BacktraceHttpAsyncTask(String url, UUID requestId, String json, ArrayList<String>
            attachments, BacktraceReport report, OnServerResponseEventListener onServerResponse,
                                  OnServerErrorEventListener onServerError,
                                  OnAfterSendEventListener afterSend) {
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

            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod("POST");

            OutputStream os = urlConnection.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            int statusCode = urlConnection.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_OK) {
                result = BacktraceSerializeHelper.backtraceResultFromJson(
                        getResponse(urlConnection)
                );
                result.setBacktraceReport(report);
                if (this.onServerResponse != null) {
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
                this.onServerError.onEvent(e);
            }
            return BacktraceResult.OnError(report, e);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                    return BacktraceResult.OnError(report, e);
                }
            }
        }
        return result;
    }

    @Override
    public void onPostExecute(BacktraceResult result) {
        if (afterSend != null) {
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