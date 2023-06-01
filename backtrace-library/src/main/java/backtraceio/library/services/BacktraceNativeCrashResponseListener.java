package backtraceio.library.services;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import backtraceio.library.BacktraceClient;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.common.BacktraceConstants;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import kotlin.text.Regex;

public class BacktraceNativeCrashResponseListener
        implements OnServerResponseEventListener {
    private static final String LOG_TAG = BacktraceNativeCrashResponseListener.
            class.getSimpleName();

    private static BacktraceNativeCrashResponseListener instance;

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
        if (databasePath.equals("")) {
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
        String regex = "/(" + BacktraceConstants.NewFolder + "|" + BacktraceConstants.PendingFolder
                + ")/";
        String minidumpOutputPath = minidumpPath.replaceAll(regex, "/completed/");
        String metaDataPath = minidumpPath.replace(BacktraceConstants.MinidumpExtension,
                BacktraceConstants.MetadataExtension);
        String metaDataCompletedPath = metaDataPath.replaceAll(regex, "/completed/");
        if (moveFile(minidumpPath, minidumpOutputPath))
            moveFile(metaDataPath, minidumpOutputPath);
    }

    private boolean moveFile(String fromPath, String toPath) {
        File from = new File(fromPath);
        if (from.exists()) {
            File to = new File(toPath);
            boolean success = from.renameTo(to);
            if (success) {
                from.delete();
                return true;
            }
        }
        BacktraceLogger.e(LOG_TAG, "Failed to move file to completed folder: "
                + fromPath);
        return false;
    }
}
