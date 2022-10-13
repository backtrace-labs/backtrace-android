package backtraceio.library.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import backtraceio.library.logger.BacktraceLogger;

public class HttpHelper {
    private static final String LOG_TAG = HttpHelper.class.getSimpleName();

    /**
     * Read response message from HTTP response
     *
     * @param urlConnection current HTTP connection
     * @return response from HTTP request
     * @throws IOException
     */
    public static String getResponseMessage(HttpURLConnection urlConnection) throws IOException {
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
