package backtraceio.library.services;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;


public class BacktraceHttpAsyncTask extends AsyncTask<Void, Void, BacktraceResult> {
    private String json;
    private UUID requestId;
    private ArrayList<String> attachments;
    private BacktraceReport report;
    private String url;
    private OnServerResponseEventListener onServerResponse = null;
    private OnServerErrorEventListener onServerError = null;

    public BacktraceHttpAsyncTask(String url, UUID requestId, String json, ArrayList<String> attachments, BacktraceReport report, OnServerResponseEventListener onServerResponse, OnServerErrorEventListener onServerError) {
        this.requestId = requestId;
        this.json = json;
        this.attachments = attachments;
        this.report = report;
        this.url = url;
        this.onServerResponse = onServerResponse;
        this.onServerError = onServerError;
    }

    // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
    @Override
    protected BacktraceResult doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result = null;

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
                result = BacktraceSerializeHelper.backtraceResultFromJson(getResponseJson(urlConnection));
                result.setBacktraceReport(report);
            } else {
                // TODO handle other http codes:
                // result = BacktraceResult.OnError(report, );
            }

        } catch (Exception e) {
            if(this.onServerError != null)
            {
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
    protected void onPostExecute(BacktraceResult result) {
        if(this.onServerResponse != null) {
            this.onServerResponse.onEvent(result);
        }
    }

    private String getResponseJson(HttpURLConnection urlConnection) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (urlConnection.getInputStream())));

        StringBuilder responseSB = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            responseSB.append(line);
        }
        br.close();
        return responseSB.toString();
    }
}