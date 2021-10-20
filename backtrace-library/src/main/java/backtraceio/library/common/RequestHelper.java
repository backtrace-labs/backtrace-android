package backtraceio.library.common;

import java.io.IOException;
import java.io.OutputStream;

import backtraceio.library.logger.BacktraceLogger;

public class RequestHelper {
    private static final transient String LOG_TAG = RequestHelper.class.getSimpleName();

    private static final String CRLF = "\r\n";
    private static final String ENCODING = "utf-8";

    /**
     * Get Content-Type of request
     *
     * @return string with content type and information about boundary
     */
    public static String getContentType() {
        return "application/json";
    }

    /**
     * Write JSON string to output data steam
     *
     * @param outputStream output data stream
     * @param json         JSON string with BacktraceData object
     * @throws IOException
     */
    public static void addJson(OutputStream outputStream, String json) throws IOException {
        if (BacktraceStringHelper.isNullOrEmpty(json)) {
            BacktraceLogger.w(LOG_TAG, "JSON is null or empty");
            return;
        }

        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }

        byte[] bytes = json.getBytes(ENCODING);
        outputStream.write(bytes);
    }

    /**
     * Write to output data stream string which ending the request
     *
     * @param outputStream output data stream
     * @throws IOException
     */
    public static void addEndOfRequest(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }

        outputStream.write(RequestHelper.CRLF.getBytes());
    }
}
