package backtraceio.library.models;

import android.content.Context;

import java.util.List;

import backtraceio.library.common.FileHelper;

public class BacktraceDataAttachmentsFileHelper {

    public static List<String> getExistingFiles (Context context, BacktraceData backtraceData) { // TODO: fix name
        return FileHelper.filterOutFiles(context, backtraceData.getAttachmentPaths());
    }
}
