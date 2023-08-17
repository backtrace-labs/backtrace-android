package backtraceio.library.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;

import backtraceio.library.logger.BacktraceLogger;

/***
 * Helper class for building multipart/form-data request
 */
public class MultiFormRequestHelper {
    private static final transient String LOG_TAG = MultiFormRequestHelper.class.getSimpleName();

    private static final String BOUNDARY = "*****";
    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private static final String ENCODING = "utf-8";

    /**
     * Get Content-Type of request
     *
     * @return string with content type and information about boundary
     */
    public static String getContentType(String boundary) {
        return "multipart/form-data;boundary=" + boundary;
    }

    public static String getContentType() {
        return getContentType(MultiFormRequestHelper.BOUNDARY);
    }

    /**
     * Write to output data stream string which ending the request
     *
     * @param outputStream output data stream
     * @throws IOException
     */
    public static void addEndOfRequest(OutputStream outputStream, String boundary) throws
            IOException {
        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }

        outputStream.write((MultiFormRequestHelper.TWO_HYPHENS + boundary +
                MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.CRLF).getBytes());
    }

    public static void addEndOfRequest(OutputStream outputStream) throws IOException {
        addEndOfRequest(outputStream, MultiFormRequestHelper.BOUNDARY);
    }

    /**
     * Write JSON string to output data steam
     *
     * @param outputStream output data stream
     * @param json         JSON string with BacktraceData object
     * @throws IOException
     */
    public static void addJson(OutputStream outputStream, String json, String boundary) throws
            IOException {
        if (BacktraceStringHelper.isNullOrEmpty(json)) {
            BacktraceLogger.w(LOG_TAG, "JSON is null or empty");
            return;
        }

        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }

        outputStream.write((MultiFormRequestHelper.TWO_HYPHENS + boundary +
                MultiFormRequestHelper.CRLF).getBytes());
        outputStream.write((MultiFormRequestHelper.getFileInfo("upload_file")).getBytes());
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());

        byte[] bytes = json.getBytes(ENCODING);
        outputStream.write(bytes);
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());
    }

    public static void addJson(OutputStream outputStream, String json) throws IOException {
        addJson(outputStream, json, MultiFormRequestHelper.BOUNDARY);
    }

    /***
     * Write files data to outputStream
     * @param outputStream output data stream
     * @param attachments list of paths to files
     * @throws IOException
     */
    public static void addFiles(OutputStream outputStream, List<String> attachments, String boundary)
            throws IOException {
        if (attachments == null || outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Attachments or output stream is null");
            return;
        }

        for (String fileAbsolutePath : attachments) {
            addFile(outputStream, fileAbsolutePath, boundary);
        }
    }

    public static void addFiles(OutputStream outputStream, List<String> attachments) throws
            IOException {
        addFiles(outputStream, attachments, MultiFormRequestHelper.BOUNDARY);
    }

    /***
     * Write mindump file in multiform data format to outputStream
     * @param outputStream output data stream
     * @param absolutePath path to minidump
     * @throws IOException
     */
    public static void addMinidump(OutputStream outputStream, String absolutePath, String boundry)
            throws IOException {
        if (BacktraceStringHelper.isNullOrEmpty(absolutePath) || outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Absolute path or output stream is null");
            return;
        }

        String fileContentType = "application/octet-stream";

        outputStream.write((MultiFormRequestHelper.TWO_HYPHENS + boundry +
                MultiFormRequestHelper.CRLF).getBytes());
        outputStream.write((MultiFormRequestHelper.getFileInfo("upload_file_minidump")
                .getBytes()));
        outputStream.write(("Content-Type: " + fileContentType + MultiFormRequestHelper.CRLF)
                .getBytes());
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());
        streamFile(outputStream, absolutePath);
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());
    }

    public static void addMinidump(OutputStream outputStream, String aboslutePath) throws
            IOException {
        addMinidump(outputStream, aboslutePath, MultiFormRequestHelper.BOUNDARY);
    }

    /***
     * Write single file in multiform data format to outputStream
     * @param outputStream output data stream
     * @param absolutePath file absolute path
     * @throws IOException
     */
    private static void addFile(OutputStream outputStream, String absolutePath, String boundary)
            throws IOException {

        if (BacktraceStringHelper.isNullOrEmpty(absolutePath) || outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Absolute path or output stream is null");
            return;
        }

        String fileContentType = URLConnection.guessContentTypeFromName(FileHelper
                .getFileNameFromPath
                        (absolutePath));

        outputStream.write((MultiFormRequestHelper.TWO_HYPHENS + boundary +
                MultiFormRequestHelper.CRLF).getBytes());
        outputStream.write((MultiFormRequestHelper.getFileInfo("attachment_" + FileHelper
                .getFileNameFromPath
                        (absolutePath))).getBytes());
        outputStream.write(("Content-Type: " + fileContentType + MultiFormRequestHelper.CRLF)
                .getBytes
                        ());
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());
        streamFile(outputStream, absolutePath);
        outputStream.write((MultiFormRequestHelper.CRLF).getBytes());

    }

    private static void addFile(OutputStream outputStream, String absolutePath) throws IOException {
        addFile(outputStream, absolutePath, MultiFormRequestHelper.BOUNDARY);
    }

    /***
     * Write file content to output data stream
     * @param outputStream output data stream
     * @param absolutePath absolute path to file
     * @throws IOException
     */
    public static void streamFile(OutputStream outputStream, String absolutePath) throws
            IOException {
        if (outputStream == null || BacktraceStringHelper.isNullOrEmpty(absolutePath)) {
            BacktraceLogger.w(LOG_TAG, "Absolute path or output stream is null");
            return;
        }
        FileInputStream fis = new FileInputStream(absolutePath);
        byte[] b = new byte[4096];
        int c;
        while ((c = fis.read(b)) != -1) {
            outputStream.write(b, 0, c);
        }
    }


    /***
     * Get string with information about file like content-disposition, name and filename
     * @param fileName filename with extension
     * @return string with file information for multiform data
     */
    private static String getFileInfo(String fileName) {
        return "Content-Disposition: form-data; name=\"" +
                fileName + "\";filename=\"" +
                fileName + "\"" + MultiFormRequestHelper.CRLF;
    }
}
