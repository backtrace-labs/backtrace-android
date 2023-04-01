package backtraceio.library.breadcrumbs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BreadcrumbsReader {

    public static List<String> readBreadcrumbLogFile(String path) throws IOException {
        BacktraceBreadcrumbs breadcrumbs = new BacktraceBreadcrumbs(path);
        File breadcrumbLogFile = new File(breadcrumbs.getBreadcrumbLogPath());

        List<String> breadcrumbLogFileData = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(breadcrumbLogFile.getAbsolutePath());

        // The encoding contains headers for the encoded data
        // We just throw away lines that don't start with "timestamp
        StringBuilder stringBuilder = new StringBuilder();
        while (inputStream.available() > 0) {
            char c = (char) inputStream.read();
            if (c == '\n') {
                String line = stringBuilder.toString();
                if (line.matches(".*timestamp.*")) {
                    breadcrumbLogFileData.add(line);
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder.append(c);
        }

        return breadcrumbLogFileData;
    }
}
