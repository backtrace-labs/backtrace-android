package backtraceio.library.models;

import android.content.Context;
import android.icu.util.Output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import backtraceio.library.common.BacktraceConstants;
import backtraceio.library.common.FileHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceReport;

public final class BacktraceNativeData {
    private String minidumpPath = "";
    private List<String> attachments = new ArrayList<>();
    public BacktraceReport report = null;
    private final String LOG_TAG = BacktraceNativeData.class.getSimpleName();

    public BacktraceNativeData(BacktraceReport report) {
        organizeAttachments(report);

        if (report == null) {
            BacktraceLogger.d(LOG_TAG, "Report cannot be null");
            return;
        }
        if (minidumpPath.equals("")) {
            BacktraceLogger.d(LOG_TAG, "No minidump found in report attachments.");
            attachments.clear();
            return;
        }

        this.report = report;
        this.minidumpPath = minidumpPath;
        if (attachments.size() > 0)
            this.attachments.addAll(attachments);
    }

    /**
     * Look through attachments to determine where they should be placed for BacktraceNativeData
     * structure
     * @param report BacktraceReport
     *
     */
    private void organizeAttachments(BacktraceReport report) {
        for (String file: report.attachmentPaths) {
            if (file.endsWith(BacktraceConstants.MinidumpExtension))
                minidumpPath = file;
            else
                attachments.add(file);
        }
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public String getMinidumpPath() {
        return minidumpPath;
    }

    public void postOutputStream(OutputStream outputStream, String boundary) throws IOException {
        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }
        if (this.minidumpPath == "") {
            BacktraceLogger.w(LOG_TAG, "No minidump to upload");
            return;
        }
        MultiFormRequestHelper.addMinidump(outputStream, this.minidumpPath, boundary);
        MultiFormRequestHelper.addFiles(outputStream, this.attachments, boundary);
        MultiFormRequestHelper.addEndOfRequest(outputStream, boundary);
    }

    public void postOutputStream(OutputStream outputStream) throws IOException {
        postOutputStream(outputStream, "*****");
    }
}
