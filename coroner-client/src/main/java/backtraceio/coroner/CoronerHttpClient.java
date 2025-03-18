package backtraceio.coroner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import backtraceio.coroner.common.Common;
import backtraceio.coroner.common.HttpClient;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.serialization.CoronerResponseGroupDeserializer;
import backtraceio.coroner.serialization.GsonWrapper;

import backtraceio.coroner.common.AndroidLogDelegate;

class CoronerHttpClient implements HttpClient {
    private static final Logger LOGGER = Logger.getLogger(CoronerResponseGroupDeserializer.class.getName());
    private final String apiUrl;
    private final String coronerToken;
    private final String ENCODING = "utf-8";
    public CoronerHttpClient(final String apiUrl, final String coronerToken) {
        this.apiUrl = apiUrl;
        this.coronerToken = coronerToken;
    }
    public CoronerApiResponse get(final String requestJson) throws CoronerHttpException, IOException {
        final HttpURLConnection urlConnection = prepareHttpRequest(requestJson);
        final int statusCode = urlConnection.getResponseCode();
        backtraceio.coroner.common.Logger.d("CoronerHttpClient", "invoked");
        backtraceio.coroner.common.Logger.d("CoronerHttpClient statusCode", String.valueOf(statusCode));
        if (statusCode != HttpURLConnection.HTTP_OK) {
            String message = getResponseMessage(urlConnection);

            backtraceio.coroner.common.Logger.d("CoronerHttpClient getResponseMessage message", message);

            message = (Common.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new CoronerHttpException(statusCode, String.format("%s: %s", statusCode, message));
        }

        final String resultJson = getResponseMessage(urlConnection);

        backtraceio.coroner.common.Logger.d("CoronerHttpClient resultJson", resultJson);


        return GsonWrapper.fromJson(
                resultJson,
                CoronerApiResponse.class);
    }

    private static String getResponseMessage(final HttpURLConnection urlConnection) throws IOException {
        LOGGER.log(Level.INFO, "Reading response from HTTP request");

        final InputStream inputStream = getInputStream(urlConnection);

        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                inputStream));

        final StringBuilder responseStringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            responseStringBuilder.append(line);
        }
        bufferedReader.close();
        return responseStringBuilder.toString();
    }

    private static InputStream getInputStream(final HttpURLConnection urlConnection) throws IOException {
        if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            return urlConnection.getInputStream();
        }
        return urlConnection.getErrorStream();
    }

    private HttpURLConnection prepareHttpRequest(final String json) throws IOException {
        final URL url = new URL(this.apiUrl);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        urlConnection.setRequestProperty("Connection", "Keep-Alive");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("X-Coroner-Token", this.coronerToken);

        DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());

        final byte[] bytes = json.getBytes(ENCODING);
        request.write(bytes);

        request.flush();
        request.close();

        return urlConnection;
    }
}
