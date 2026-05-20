package backtraceio.library.breadcrumbs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Test helper that extracts breadcrumb JSON records from the on-disk QueueFile log.
 */
public class BreadcrumbsReader {

    /**
     * Reads the breadcrumb log file and returns each breadcrumb as a JSON string.
     *
     * <p>Lines are trimmed to their first {@code '{'}} to strip QueueFile framing bytes,
     * then kept only if they parse as JSON and contain a {@code timestamp} field. This
     * filters out framing noise and orphaned sub-objects exposed by rollover.</p>
     *
     * @param path directory containing the breadcrumb log
     * @return breadcrumb records in file order
     */
    public static List<String> readBreadcrumbLogFile(String path) throws IOException {
        BacktraceBreadcrumbs breadcrumbs = new BacktraceBreadcrumbs(path);
        File breadcrumbLogFile = new File(breadcrumbs.getBreadcrumbLogPath());

        List<String> breadcrumbLogFileData = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(breadcrumbLogFile.getAbsolutePath());

        StringBuilder stringBuilder = new StringBuilder();
        while (inputStream.available() > 0) {
            char c = (char) inputStream.read();
            if (c == '\n') {
                String line = stringBuilder.toString();
                int braceStart = line.indexOf('{');
                if (braceStart >= 0) {
                    String candidate = line.substring(braceStart);
                    try {
                        JSONObject parsed = new JSONObject(candidate);
                        if (parsed.has("timestamp")) {
                            breadcrumbLogFileData.add(candidate);
                        }
                    } catch (JSONException ignored) {
                    }
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder.append(c);
        }

        return breadcrumbLogFileData;
    }
}
