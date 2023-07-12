package backtraceio.backtraceio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.common.URLRequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.HttpException;

public class CustomRequestHandler extends URLRequestHandler {
    CustomRequestHandler(BacktraceCredentials credentials) {
        super(credentials);
    }
    private static final String LOG_TAG = CustomRequestHandler.class.getSimpleName();
    private static final int CHUNK_SIZE = 128 * 1024;

    private HttpURLConnection setupConnection(String serverUrl) throws IOException {
        HttpURLConnection urlConnection = null;

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

        return urlConnection;
    }

    private static String getResponse(HttpURLConnection urlConnection) throws IOException {
        InputStream inputStream;
        if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = urlConnection.getInputStream();
        } else {
            inputStream = urlConnection.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder responseSB = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            responseSB.append(line);
        }
        br.close();
        return responseSB.toString();
    }

    private BacktraceResult getResult(HttpURLConnection urlConnection, BacktraceData data) throws IOException, HttpException {
        BacktraceResult result;
        int statusCode = urlConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            result = BacktraceSerializeHelper.backtraceResultFromJson(
                    getResponse(urlConnection));
            result.setBacktraceReport(data.report);
        } else {
            String message = getResponse(urlConnection);
            message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
        }
        return result;
    }

    private BacktraceResult getResult(HttpURLConnection urlConnection, BacktraceNativeData data) throws IOException, HttpException {
        BacktraceResult result;
        int statusCode = urlConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            result = BacktraceSerializeHelper.backtraceResultFromJson(
                    getResponse(urlConnection));
            result.setBacktraceReport(data.report);
        } else {
            String message = getResponse(urlConnection);
            message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
        }
        return result;
    }

    @Override
    public BacktraceResult onRequest(BacktraceData data) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result;
        try {
            urlConnection = setupConnection(jsonURL);
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            String json = BacktraceSerializeHelper.toJson(data);
            List<String> attachments = data.getAttachments();
            MultiFormRequestHelper.addJson(request, json);
            MultiFormRequestHelper.addFiles(request, attachments);
            MultiFormRequestHelper.addEndOfRequest(request);
            request.flush();
            request.close();
            result = getResult(urlConnection, data);
        } catch (Exception e) {
            e.printStackTrace();
            result = BacktraceResult.OnError(data.report, e);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    result = BacktraceResult.OnError(data.report, e);
                }
            }
        }
        return result;
    }

    @Override
    public BacktraceResult onNativeRequest(BacktraceNativeData data) {
        Log.d(LOG_TAG,"Attachments count: " + data.getAttachments().size());
        List<String> files = data.getAttachments();
        for (String file: files) {
            Log.d(LOG_TAG, "Attachment: " + file);
        }
        HttpURLConnection urlConnection = null;
        BacktraceResult result;
        Log.d(LOG_TAG, "Native request made to the custom request handler");
        try {
            urlConnection = setupConnection(minidumpURL);
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            data.postOutputStream(request);
            request.flush();
            request.close();

            result = getResult(urlConnection, data);
        } catch (Exception e) {
            e.printStackTrace();
            result = BacktraceResult.OnError(data.report, e);
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    result = BacktraceResult.OnError(data.report, e);
                }
            }
        }
        return result;
    }
}
