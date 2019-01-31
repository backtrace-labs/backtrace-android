package backtraceio.library.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;

public class MultiFormRequestHelper {
    private static final String BOUNDARY = "*****";
    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";

    public static String getContentType() {
        return "multipart/form-data;boundary=" + MultiFormRequestHelper.BOUNDARY;
    }

    public static void addEndOfRequest(OutputStream request) throws IOException {
        if (request == null) {
            return;
        }

        request.write((MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY +
                MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.CRLF).getBytes());
    }

    public static void addJson(OutputStream request, String json) throws IOException {
        if (json == null || json.isEmpty()) {
            return;
        }
        request.write((MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY +
                MultiFormRequestHelper.CRLF).getBytes());
        request.write((MultiFormRequestHelper.getFileInfo("upload_file")).getBytes());
        request.write((MultiFormRequestHelper.CRLF).getBytes());

        byte[] bytes = json.getBytes("utf-8");
        request.write(bytes);
        request.write((MultiFormRequestHelper.CRLF).getBytes());
    }

    public static void addFiles(OutputStream request, List<String> attachments) throws IOException {
        if (attachments == null) {
            return;
        }

        for (String fileAbsolutePath : attachments) {
            if (MultiFormRequestHelper.isFilePathValid(fileAbsolutePath)) {
                continue;
            }
            addFile(request, fileAbsolutePath);
        }
    }

    private static void addFile(OutputStream request, String absolutePath) throws IOException {
        String fileContentType = URLConnection.guessContentTypeFromName(getFileNameFromPath
                (absolutePath));

        request.write((MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY +
                MultiFormRequestHelper.CRLF).getBytes());
        request.write((MultiFormRequestHelper.getFileInfo("attachment_" + getFileNameFromPath
                (absolutePath))).getBytes());
        request.write(("Content-Type: " + fileContentType + MultiFormRequestHelper.CRLF).getBytes
                ());
        request.write((MultiFormRequestHelper.CRLF).getBytes());
        streamFile(request, absolutePath);
        request.write((MultiFormRequestHelper.CRLF).getBytes());

    }

    public static void streamFile(OutputStream outputStream, String absolutePath) throws
            IOException {
        FileInputStream fis = new FileInputStream(absolutePath);
        byte[] b = new byte[4096];
        int c;
        while ((c = fis.read(b)) != -1) {
            outputStream.write(b, 0, c);
        }
    }

    private static String getFileNameFromPath(String absolutePath) {
        return absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
    }

    private static String getFileInfo(String fileName) {
        return "Content-Disposition: form-data; name=\"" +
                fileName + "\";filename=\"" +
                fileName + "\"" + MultiFormRequestHelper.CRLF;
    }

    private static boolean isFilePathValid(String filePath) {
        return filePath == null || filePath.isEmpty() || !isFileExists(filePath);
    }

    private static boolean isFileExists(String absoluteFilePath) {
        return new File(absoluteFilePath).exists();
    }
}
