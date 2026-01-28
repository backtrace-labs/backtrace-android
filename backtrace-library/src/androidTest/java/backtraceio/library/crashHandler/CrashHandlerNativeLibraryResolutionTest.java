package backtraceio.library.crashHandler;

import static org.junit.Assert.assertEquals;

import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CrashHandlerNativeLibraryResolutionTest {

    private static final String LIB = "libbacktrace-native.so";

    private File tempDir(String name) {
        File cache =
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        File d = new File(cache, name);
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    private File makeApk(File dir, String name, String abi, boolean includeLib) throws Exception {
        File apk = new File(dir, name);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apk))) {
            if (includeLib) {
                String entry = "lib/" + abi + "/" + LIB;
                zos.putNextEntry(new ZipEntry(entry));
                zos.write(new byte[] {1, 2, 3, 4});
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                zos.write(0);
                zos.closeEntry();
            }
        }
        return apk;
    }

    private static String getEnv(List<String> env, String key) {
        String prefix = key + "=";
        for (String kv : env) {
            if (kv.startsWith(prefix)) return kv.substring(prefix.length());
        }
        return null;
    }

    @Test
    public void usesSplitWhenBaseDoesNotContainLib() throws Exception {
        final String abi = AbiHelper.getCurrentAbi();
        File root = tempDir("bt_split_pref");
        File base = makeApk(root, "base.apk", abi, false);
        File split = makeApk(root, "split_config." + abi + ".apk", abi, true);

        ApplicationInfo ai = new ApplicationInfo();
        ai.sourceDir = base.getAbsolutePath();
        ai.splitSourceDirs = new String[] {split.getAbsolutePath()};
        ai.nativeLibraryDir = "/nonexistent";

        CrashHandlerConfiguration cfg = new CrashHandlerConfiguration();
        List<String> env = cfg.getCrashHandlerEnvironmentVariables(ai);

        String libPath = getEnv(env, CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        String apkLib = split.getAbsolutePath() + "!/lib/" + abi + "/" + LIB;
        assertEquals(apkLib, libPath);

        String classPath = getEnv(env, "CLASSPATH");
        assertEquals(base.getAbsolutePath(), classPath);
    }

    @Test
    public void prefersExtractedOverApkContainers() throws Exception {
        final String abi = AbiHelper.getCurrentAbi();
        File root = tempDir("bt_extracted_pref");
        File base = makeApk(root, "base.apk", abi, false);
        File split = makeApk(root, "split_config." + abi + ".apk", abi, true);

        File nativeDir = new File(root, "lib/" + abi);
        //noinspection ResultOfMethodCallIgnored
        nativeDir.mkdirs();
        File extracted = new File(nativeDir, LIB);
        try (FileOutputStream fos = new FileOutputStream(extracted)) {
            fos.write(new byte[] {9, 9, 9});
        }

        ApplicationInfo ai = new ApplicationInfo();
        ai.sourceDir = base.getAbsolutePath();
        ai.splitSourceDirs = new String[] {split.getAbsolutePath()};
        ai.nativeLibraryDir = nativeDir.getAbsolutePath();

        CrashHandlerConfiguration cfg = new CrashHandlerConfiguration();
        List<String> env = cfg.getCrashHandlerEnvironmentVariables(ai);

        String libPath = getEnv(env, CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertEquals(extracted.getAbsolutePath(), libPath);

        String classPath = getEnv(env, "CLASSPATH");
        assertEquals(base.getAbsolutePath(), classPath);
    }

    @Test
    public void prefersBaseWhenBaseContainsLib() throws Exception {
        final String abi = AbiHelper.getCurrentAbi();
        File root = tempDir("bt_base_pref");
        File base = makeApk(root, "base.apk", abi, true);
        File split = makeApk(root, "split_config." + abi + ".apk", abi, true);

        ApplicationInfo ai = new ApplicationInfo();
        ai.sourceDir = base.getAbsolutePath();
        ai.splitSourceDirs = new String[] {split.getAbsolutePath()};
        ai.nativeLibraryDir = "/nonexistent";

        CrashHandlerConfiguration cfg = new CrashHandlerConfiguration();
        List<String> env = cfg.getCrashHandlerEnvironmentVariables(ai);

        String libPath = getEnv(env, CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        String apkLib = base.getAbsolutePath() + "!/lib/" + abi + "/" + LIB;
        assertEquals(apkLib, libPath);
    }

    @Test
    public void fallsBackToBasePathWhenNoContainerHasLib() throws Exception {
        final String abi = AbiHelper.getCurrentAbi();
        File root = tempDir("bt_fallback");
        File base = makeApk(root, "base.apk", abi, false);

        ApplicationInfo ai = new ApplicationInfo();
        ai.sourceDir = base.getAbsolutePath();
        ai.splitSourceDirs = null;
        ai.nativeLibraryDir = "/nonexistent";

        CrashHandlerConfiguration cfg = new CrashHandlerConfiguration();
        List<String> env = cfg.getCrashHandlerEnvironmentVariables(ai);

        String libPath = getEnv(env, CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        String apkLib = base.getAbsolutePath() + "!/lib/" + abi + "/" + LIB;
        assertEquals(apkLib, libPath);
    }
}
