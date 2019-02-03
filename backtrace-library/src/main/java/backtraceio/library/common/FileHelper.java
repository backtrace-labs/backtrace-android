package backtraceio.library.common;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FileHelper {
    public static boolean isPathToInternalStorage(Context context, String path) {
        if (context == null) {
            return false;
        }
        return path.startsWith(context.getFilesDir().getAbsolutePath());
    }

    public static ArrayList<String> filterOutFiles(Context context, List<String> paths) {
        paths = new ArrayList<>(new HashSet<>(paths)); // get only unique elements

        ArrayList<String> result = new ArrayList<>();

        for (String path : paths) {
            if (isFilePathInvalid(path) || (!isPathToInternalStorage(context, path) &&
                    !PermissionHelper.isPermissionForReadExternalStorageGranted(context))) {
                Log.e("Backtrace.io", String.format("Path for file '%s' is incorrect or permission READ_EXTERNAL_STORAGE is not granted.", path));
                continue;
            }

            result.add(path);
        }
        return result;
    }

    private static boolean isFilePathInvalid(String filePath) {
        return filePath == null || filePath.isEmpty() || !isFileExists(filePath);
    }

    private static boolean isFileExists(String absoluteFilePath) {
        return new File(absoluteFilePath).exists();
    }
}
