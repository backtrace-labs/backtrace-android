package backtraceio.backtraceio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.common.URLRequestHandler;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceBaseData;
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

    private BacktraceResult getResult(HttpURLConnection urlConnection, BacktraceBaseData data) throws IOException, HttpException {
        int statusCode = urlConnection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            String message = getResponse(urlConnection);
            message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
        }
        BacktraceResult result = BacktraceSerializeHelper.backtraceResultFromJson(getResponse(urlConnection));
        result.setBacktraceReport(data.report);
        return result;
    }

    @Override
    public BacktraceResult onRequest(BacktraceData data) {
        BacktraceResult result = sendData(data, jsonURL);
        return result;
    }

    @Override
    public BacktraceResult onNativeRequest(BacktraceNativeData data) {
        BacktraceResult result = sendData(data, minidumpURL);
        return result;
    }

    public BacktraceResult sendData(BacktraceBaseData data, String URL) {
        HttpURLConnection urlConnection = null;
        BacktraceResult result;
        try {
            urlConnection = setupConnection(URL);
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            if (data instanceof BacktraceNativeData)
                postNativeData((BacktraceNativeData) data, request, null);
            else if (data instanceof BacktraceData)
                postJsonData((BacktraceData) data, request, null);
            request.flush();
            request.close();
            result = getResult(urlConnection, data);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            result = BacktraceResult.OnError(data.report, e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result;
    }

    public void postNativeData(BacktraceNativeData data, OutputStream outputStream, String boundary)
            throws IOException {
        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }
        if (BacktraceStringHelper.isNullOrEmpty(data.getMinidumpPath())) {
            BacktraceLogger.w(LOG_TAG, "No minidump to upload");
            return;
        }
        if (BacktraceStringHelper.isNullOrEmpty(boundary)) {
            boundary = "*********";
        }
        MultiFormRequestHelper.addMinidump(outputStream, data.getMinidumpPath(), boundary);
        MultiFormRequestHelper.addFiles(outputStream, data.getAttachments(), boundary);
        MultiFormRequestHelper.addAttributes(outputStream, data.attributes, boundary);
        MultiFormRequestHelper.addEndOfRequest(outputStream, boundary);
    }

    public void postJsonData(BacktraceData data, OutputStream outputStream, String boundary)
            throws IOException {
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();
        MultiFormRequestHelper.addJson(outputStream, json);
        MultiFormRequestHelper.addFiles(outputStream, attachments);
        MultiFormRequestHelper.addEndOfRequest(outputStream);
    }
}
