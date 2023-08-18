package backtraceio.library.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.common.BacktraceConstants;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceNativeCrashResponseListener
        implements OnServerResponseEventListener {
    private static final String LOG_TAG = BacktraceNativeCrashResponseListener.
            class.getSimpleName();

    private final static BacktraceNativeCrashResponseListener instance = new BacktraceNativeCrashResponseListener();
    private final static FilenameFilter minidumpFilter = (dir, name) -> name.endsWith(BacktraceConstants.MinidumpExtension);

    private BacktraceNativeCrashResponseListener() {
    }

    @Override
    public void onEvent(BacktraceResult backtraceResult) {
        if (backtraceResult.status == BacktraceResultStatus.Ok) {
            // Get the minidump out
            final BacktraceReport report = backtraceResult.getBacktraceReport();
            if (report != null) {
                final List<String> attachments = report.attachmentPaths;
                for (String attachment : attachments) {
                    if (attachment.endsWith(BacktraceConstants.MinidumpExtension)) {
                        moveToCompleted(attachment);
                        break;
                    }
                }
            }
        }
    }

    public static void uploadMinidumps(BacktraceBase client, String databasePath) {
        if (client == null) {
            BacktraceLogger.e(LOG_TAG, "Backtrace client can't be null");
            return;
        }
        if (databasePath.equals("")) {
            BacktraceLogger.e(LOG_TAG, "Database path cannot be null");
            return;
        }
        final Thread sender = new Thread(() -> {
            final List<File> minidumps = scanForNewMinidumps(databasePath);
            if (minidumps.size() > 0) {
                for (File minidump : minidumps) {
                    final BacktraceReport report = createBacktraceReport(minidump, databasePath);
                    client.send(report, instance);
                }
            }
        });
        sender.run();
    }

    private static List<File> scanForNewMinidumps(String databasePath) {
        final List<File> minidumps = new ArrayList<>();
        minidumps.addAll(getFiles(databasePath + "/" + BacktraceConstants.NewFolder));
        minidumps.addAll(getFiles(databasePath + "/" + BacktraceConstants.PendingFolder));
        return minidumps;
    }

    private static List<File> getFiles(String dirPath) {
        final String[] dirFiles = new File(dirPath).list(minidumpFilter);
        final List<File> result = new ArrayList<>();
        if (dirFiles != null) {
            for (String file : dirFiles) {
                result.add(new File(dirPath + "/" + file));
            }
        }
        return result;
    }

    private static BacktraceReport createBacktraceReport(File minidump, String databasePath) {
        final BacktraceReport report = new BacktraceReport("");
        report.attachmentPaths.add(minidump.getAbsolutePath());
        final String attachmentPath = databasePath + "/" + BacktraceConstants.AttachmentsFolder + "/"
                + minidump.getName().replace(BacktraceConstants.MinidumpExtension, "");
        final File attachmentsDir = new File(attachmentPath);
        if (attachmentsDir.isDirectory()) {
            for (String file : attachmentsDir.list()) {
                report.attachmentPaths.add(attachmentsDir.getAbsolutePath() + "/" + file);
            }
        }
        return report;
    }

    private void moveToCompleted(String minidumpPath) {
        final String regex = "/(" + BacktraceConstants.NewFolder + "|" + BacktraceConstants.PendingFolder
                + ")/";
        final String minidumpOutputPath = minidumpPath.replaceAll(regex, "/" + BacktraceConstants.CompletedFolder + "/");
        final String metaDataPath = minidumpPath.replace(BacktraceConstants.MinidumpExtension,
                BacktraceConstants.MetadataExtension);
        final String metaDataCompletedPath = metaDataPath.replaceAll(regex, "/" + BacktraceConstants.CompletedFolder + "/");
        if (moveFile(minidumpPath, minidumpOutputPath))
            moveFile(metaDataPath, metaDataCompletedPath);
    }

    private boolean moveFile(String fromPath, String toPath) {
        final File from = new File(fromPath);
        if (from.exists()) {
            final File to = new File(toPath);
            final boolean success = from.renameTo(to);
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
