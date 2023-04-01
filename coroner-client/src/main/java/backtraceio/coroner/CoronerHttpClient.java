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
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.serialization.CoronerResponseGroupDeserializer;
import backtraceio.coroner.serialization.GsonWrapper;

class CoronerHttpClient {
    private static final Logger LOGGER = Logger.getLogger(CoronerResponseGroupDeserializer.class.getName());
    private final String apiUrl;
    private final String coronerToken;
    private final String ENCODING = "utf-8";

    public CoronerHttpClient(String apiUrl, String coronerToken) {
        this.apiUrl = apiUrl;
        this.coronerToken = coronerToken;
    }

    public CoronerApiResponse get(String json) throws CoronerHttpException, IOException {
        HttpURLConnection urlConnection = prepareHttpRequest(json);
        int statusCode = urlConnection.getResponseCode();

        if (statusCode != HttpURLConnection.HTTP_OK) {
            String message = getResponseMessage(urlConnection);
            message = (Common.isNullOrEmpty(message)) ?
                    urlConnection.getResponseMessage() : message;
            throw new CoronerHttpException(statusCode, String.format("%s: %s", statusCode, message));
        }

        String resultJson = getResponseMessage(urlConnection);

        return GsonWrapper.fromJson(
                resultJson,
                CoronerApiResponse.class);
    }

    private static String getResponseMessage(HttpURLConnection urlConnection) throws IOException {
        LOGGER.log(Level.INFO, "Reading response from HTTP request");

        InputStream inputStream = getInputStream(urlConnection);

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

    private static InputStream getInputStream(HttpURLConnection urlConnection) throws IOException {
        if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST)
            return urlConnection.getInputStream();
        return urlConnection.getErrorStream();
    }

    private HttpURLConnection prepareHttpRequest(String json) throws IOException {
        URL url = new URL(this.apiUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        urlConnection.setRequestProperty("Connection", "Keep-Alive");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("X-Coroner-Token", this.coronerToken);

        DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());

        byte[] bytes = json.getBytes(ENCODING);
        request.write(bytes);

        request.flush();
        request.close();

        return urlConnection;
    }
}
