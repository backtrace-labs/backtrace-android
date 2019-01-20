package backtraceio.library.common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

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
        request.writeBytes("Content-Disposition: form-data; name=\"" +
                "upload_file" + "\";filename=\"" +
                "upload_file" + "\"" + MultiFormRequestHelper.CRLF);
        request.writeBytes(MultiFormRequestHelper.CRLF);

        byte[] bytes = json.getBytes("utf-8");
        request.write(bytes);
        request.writeBytes(MultiFormRequestHelper.CRLF);
    }
}
