package backtraceio.library.models;

import android.content.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backtraceio.library.common.BacktraceConstants;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.FileHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceReport;

public final class BacktraceNativeData extends BacktraceBaseData {
    private final String minidumpPath;
    private final List<String> attachments = new ArrayList<>();
    public final BacktraceReport report;
    private final String LOG_TAG = BacktraceNativeData.class.getSimpleName();

    public BacktraceNativeData(BacktraceReport report) {
        if (report == null) {
            BacktraceLogger.d(LOG_TAG, "Report cannot be null");
            this.report = null;
            this.minidumpPath = null;
            return;
        }
        organizeAttachments(report);
        this.minidumpPath = getMinidumpPath(report);
        if (BacktraceStringHelper.isNullOrEmpty(minidumpPath)) {
            BacktraceLogger.d(LOG_TAG, "No minidump found in report attachments.");
            attachments.clear();
            this.report = null;
            return;
        }

        this.report = report;
        this.attributes = new HashMap<>();
        this.attributes.putAll(new HashMap<String, String>() {{
            put("agent", agent);
            put("agentVersion", agentVersion);
            put("lang", "minidump");
        }});
    }

    /**
     * Look through attachments to determine where they should be placed for BacktraceNativeData
     * structure
     * @param report BacktraceReport
     *
     */
    private void organizeAttachments(BacktraceReport report) {
        final List<String> attachments = report.attachmentPaths;
        final List<String> nativeAttachments = filterList("/crashpad/attachments/", attachments);
        this.attachments.addAll(nativeAttachments);
    }

    /**
     * Parse report for minidump attachment
     * @param report - Backtrace Report to parse
     * @return - Absolute string path to the minidump or empty string if none found
     */
    private String getMinidumpPath(BacktraceReport report) {
        final List<String> minidumps = filterList(BacktraceConstants.MinidumpExtension, report.attachmentPaths);
        if (minidumps.size() != 1) {
            return "";
        }
        return minidumps.get(0);
    }

    /**
     * Filter list removing matching items
     * @param regexPattern - Pattern to ignore
     * @param list - List of items to parse
     * @return filtered List<String>
     */
    private List<String> filterList(String regexPattern, List<String> list) {
        final Pattern pattern = Pattern.compile(regexPattern);
        final List<String> result = new ArrayList<>();
        for (String element: list) {
            Matcher matcher = pattern.matcher(element);
            if (matcher.find())
                result.add(element);
        }
        return result;
    }

    /**
     *
     * @return A list of attachment paths other than the minidump
     */
    public List<String> getAttachments() {
        return attachments;
    }

    /**
     *
     * @return The absolute path to the minidump
     */
    public String getMinidumpPath() {
        return minidumpPath;
    }
}
