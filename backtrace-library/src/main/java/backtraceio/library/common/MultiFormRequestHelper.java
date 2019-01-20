package backtraceio.library.common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MultiFormRequestHelper {
    private static final String BOUNDARY =  "*****";
    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";

    public static String getContentType()
    {
        return "multipart/form-data;boundary=" + MultiFormRequestHelper.BOUNDARY;
    }

    public static void addEndOfRequest(DataOutputStream request) throws IOException
    {
        request.writeBytes(MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY +
                MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.CRLF);
    }

    public static void addJson(DataOutputStream request, String json) throws IOException
    {
        if (json == null || json.equals(""))
        {
            return;
        }
        request.writeBytes(MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY + MultiFormRequestHelper.CRLF);
        request.writeBytes(MultiFormRequestHelper.getFileInfo("upload_file"));
        request.writeBytes(MultiFormRequestHelper.CRLF);

        byte[] bytes = json.getBytes("utf-8");
        request.write(bytes);
        request.writeBytes(MultiFormRequestHelper.CRLF);
    }

    public static void addFiles(DataOutputStream request, List<String> attachments) throws IOException
    {
        if (attachments == null)
        {
            return;
        }

        for(String filePath : attachments) {
            addFile(request, filePath);
        }
    }

    public static void addFile(DataOutputStream request, String absolutePath) throws IOException
    {
        FileInputStream fis = new FileInputStream(absolutePath);

        byte[] b = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = fis.read(b)) != -1) {
            os.write(b, 0, c);
        }
        byte[] bytes =  os.toByteArray();
        request.writeBytes(MultiFormRequestHelper.TWO_HYPHENS + MultiFormRequestHelper.BOUNDARY + MultiFormRequestHelper.CRLF);
        request.writeBytes(MultiFormRequestHelper.getFileInfo("attachment_" + getFileNameFromPath(absolutePath)));
        request.writeBytes("Content-Type: application/octet-stream" + MultiFormRequestHelper.CRLF);
        request.writeBytes(MultiFormRequestHelper.CRLF);
        request.write(bytes);
        request.writeBytes(MultiFormRequestHelper.CRLF);

    }

    private static String getFileNameFromPath(String absolutePath)
    {
        return absolutePath.substring(absolutePath.lastIndexOf("/")+1);
    }

    private static String getFileInfo(String fileName){
        return "Content-Disposition: form-data; name=\"" +
                 fileName + "\";filename=\"" +
                 fileName + "\"" + MultiFormRequestHelper.CRLF;
    }
}
