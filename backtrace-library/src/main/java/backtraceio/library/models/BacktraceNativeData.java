package backtraceio.library.models;

import android.content.Context;
import android.icu.util.Output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import backtraceio.library.common.FileHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceNativeData {
    private String minidumpPath;
    private List<String> attachments;
    private Context context;
    public BacktraceReport report;
    private String LOG_TAG = BacktraceNativeData.class.getSimpleName();

    public BacktraceNativeData(Context context, BacktraceReport report) {
        if (report == null)
            return;

        List<String> files = FileHelper.filterOutFiles(context, report.attachmentPaths);
        List<String> attachments = new ArrayList<>();
        String minidumpPath = "";
        for (String file: files) {
            if (file.endsWith(".dmp"))
                minidumpPath = file;
            else
                attachments.add(file);
        }

        if (minidumpPath == "")
            return;
        this.report = report;
        this.attachments = new ArrayList<>();
        this.minidumpPath = minidumpPath;
        if (attachments.size() > 0)
            this.attachments.addAll(attachments);
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public String getMinidumpPath() {
        return minidumpPath;
    }

    public void postOutputStream(OutputStream outputStream, String boundry) throws IOException {
        if (outputStream == null) {
            BacktraceLogger.w(LOG_TAG, "Output stream is null");
            return;
        }
        if (this.minidumpPath == "") {
            BacktraceLogger.w(LOG_TAG, "No minidump to upload");
            return;
        }
        MultiFormRequestHelper.addMinidump(outputStream, this.minidumpPath, boundry);
        MultiFormRequestHelper.addFiles(outputStream, this.attachments, boundry);
        MultiFormRequestHelper.addEndOfRequest(outputStream, boundry);
    }

    public void postOutputStream(OutputStream outputStream) throws IOException {
        postOutputStream(outputStream, "*****");
    }
}
