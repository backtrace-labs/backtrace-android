package backtraceio.library.services;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceNativeCrashResponseListener
        implements OnServerResponseEventListener {
    private static transient final String LOG_TAG = BacktraceNativeCrashResponseListener.
            class.getSimpleName();

    private static BacktraceNativeCrashResponseListener instance;
    private static BacktraceClient client;
    private static String databasePath;

    private BacktraceNativeCrashResponseListener() {}

    @Override
    public void onEvent(BacktraceResult backtraceResult) {
        if (backtraceResult.status == BacktraceResultStatus.Ok) {
            // Get the minidump out
            BacktraceReport report = backtraceResult.getBacktraceReport();
            if (report != null) {
                List<String> attachments = report.attachmentPaths;
                for (String attachment : attachments) {
                    if (attachment.endsWith(".dmp")) {
                        moveToCompleted(attachment);
                        break;
                    }
                }
            }
        }
    }

    public static void UploadMinidumps(BacktraceBase client, String databasePath) {
        if (client == null) {
            BacktraceLogger.e(LOG_TAG, "Backtrace client can't be null");
            return;
        }
        if (databasePath == "") {
            BacktraceLogger.e(LOG_TAG, "Database path cannot be null");
            return;
        }
        if (instance == null) {
            instance = new BacktraceNativeCrashResponseListener();
        }
        Thread sender = new Thread(() -> {
            List<File> minidumps = scanForNewMinidumps(databasePath);
            if (minidumps.size() > 0) {
                for (File minidump: minidumps) {
                    BacktraceReport report = createBacktraceReport(minidump, databasePath);
                    client.send(report, instance);
                }
            }
        });
        sender.run();
    }

    private static List<File> scanForNewMinidumps(String databasePath) {
        List<File> minidumps = new ArrayList<>();
        FilenameFilter minidumpFilter = (dir, name) -> name.endsWith(".dmp");
        String[] newFiles = new File(databasePath + "/new").list(minidumpFilter);
        String[] pendFiles = new File(databasePath + "/pending").list(minidumpFilter);
        if (newFiles != null) {
            for (String file : newFiles) {
                minidumps.add(new File(databasePath + "/new/" + file));
            }
        }
        if (pendFiles != null) {
            for (String file : pendFiles) {
                minidumps.add(new File(databasePath + "/pending/" + file));
            }
        }
        return minidumps;
    }

    private static BacktraceReport createBacktraceReport(File minidump, String databasePath) {
        BacktraceReport report = new BacktraceReport("");
        report.attachmentPaths.add(minidump.getAbsolutePath());
        String attachmentPath = databasePath + "/attachments/"
                + minidump.getName().replace(".dmp", "");
        File attachmentsDir = new File(attachmentPath);
        if (attachmentsDir.isDirectory()) {
            for (String file: attachmentsDir.list()) {
                report.attachmentPaths.add(attachmentsDir.getAbsolutePath() + "/" + file);
            }
        }
        return report;
    }

    private void moveToCompleted(String minidumpPath) {
        String outputPath = minidumpPath.replace("/new/", "/completed/")
                .replace("/pending/", "/completed/");
        File from = new File(minidumpPath);
        File to = new File(outputPath);
        if (from.exists()) {
            boolean success = from.renameTo(to);
            if (!success) {
                BacktraceLogger.e(LOG_TAG, "Failed to move minidump to completed folder: "
                        + minidumpPath);
            } else {
                from.delete();
            }
        }
        from = new File(minidumpPath.replace(".dmp", ".meta"));
        to = new File(outputPath.replace(".dmp", ".meta"));
        if (from.exists()) {
            boolean success = from.renameTo(to);
            if (!success) {
                BacktraceLogger.e(LOG_TAG, "" + "Failed to move: " + from.getAbsolutePath()
                        + " to: " + to.getAbsolutePath());
            } else {
                from.delete();
            }
        }
    }
}
