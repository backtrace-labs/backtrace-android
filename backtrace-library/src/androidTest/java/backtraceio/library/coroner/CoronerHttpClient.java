package backtraceio.library.coroner;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.RequestHelper;
import backtraceio.library.http.HttpHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.types.HttpException;

class CoronerHttpClient {
    private final String apiUrl;
    private final String coronerToken;

    public CoronerHttpClient(String apiUrl, String coronerToken) {
        this.apiUrl = apiUrl;
        this.coronerToken = coronerToken;
    }

    public CoronerApiResponse get(String json) throws IOException, HttpException {
        HttpURLConnection urlConnection = prepareHttpRequest(json);
        int statusCode = urlConnection.getResponseCode();

        if (statusCode != HttpURLConnection.HTTP_OK) {
            String message = HttpHelper.getResponseMessage(urlConnection);
            message = (BacktraceStringHelper.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new HttpException(statusCode, String.format("%s: %s", statusCode, message));
        }

        String resultJson = HttpHelper.getResponseMessage(urlConnection);
        return BacktraceSerializeHelper.fromJson(resultJson, CoronerApiResponse.class);
    }

    private HttpURLConnection prepareHttpRequest(String json) throws IOException {
        URL url = new URL(this.apiUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        urlConnection.setRequestProperty("Connection", "Keep-Alive");
        urlConnection.setRequestProperty("Content-Type", RequestHelper.getContentType());
        urlConnection.setRequestProperty("X-Coroner-Token", this.coronerToken);

        DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());

        RequestHelper.addJson(request, json);

        request.flush();
        request.close();

        return urlConnection;
    }
}
