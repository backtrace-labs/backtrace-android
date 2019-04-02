package backtraceio.library.common;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

/**
 * Helper class for access to files
 */
public class FileHelper {

    /***
     * Get file name with extension from file path
     * @param absolutePath absolute path to file
     * @return file name with extension
     */
    static String getFileNameFromPath(String absolutePath) {
        return absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
    }

    /***
     * Remove from path list invalid paths like empty or incorrect paths or not existing files
     * @param context application context
     * @param paths list of paths to files
     * @return filtered list of file paths
     */
    public static ArrayList<String> filterOutFiles(Context context, List<String> paths) {
        paths = new ArrayList<>(new HashSet<>(paths)); // get only unique elements

        ArrayList<String> result = new ArrayList<>();

        for (String path : paths) {
            if (isFilePathInvalid(path) || (!isPathToInternalStorage(context, path) &&
                    !PermissionHelper.isPermissionForReadExternalStorageGranted(context))) {
                Log.e("Backtrace.io", String.format("Path for file '%s' is incorrect or " +
                        "permission READ_EXTERNAL_STORAGE is not granted.", path));
                continue;
            }

            result.add(path);
        }
        return result;
    }

    /**
     * Get file extension
     * @param file file whose extension will be returned
     * @return file extension
     */
    public static String getFileExtension(File file) {

        String name = file.getName().substring(Math.max(file.getName().lastIndexOf('/'),
                file.getName().lastIndexOf('\\')) < 0 ? 0 : Math.max(file.getName().lastIndexOf('/'),
                file.getName().lastIndexOf('\\')));
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1); // doesn't return "." with extension
    }

    /***
     * Check does file path is invalid, null, empty or file not exists
     * @param filePath
     * @return true if path is invalid
     */
    private static boolean isFilePathInvalid(String filePath) {
        return filePath == null || filePath.isEmpty() || !isFileExists(filePath);
    }

    /***
     * Check does file exist
     * @param absoluteFilePath
     * @return true if file exists
     */
    public static boolean isFileExists(String absoluteFilePath) {
        return new File(absoluteFilePath).exists();
    }

    /***
     * Check does path is path to application internal storage
     * @param context application context
     * @param path file path
     * @return true if path is internal storage
     */
    private static boolean isPathToInternalStorage(Context context, String path) {
        if (context == null) {
            return false;
        }
        return path.startsWith(context.getFilesDir().getAbsolutePath());
    }

    public static String readFile(File file){
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder sb = new StringBuilder();

            while (scanner.hasNext()) {
                sb.append(scanner.nextLine());
            }

            scanner.close();

            return sb.toString();
        }
        catch (Exception e){
            Log.e("Backtrace.IO", e.getMessage());
            return null;
        }
    }
}
