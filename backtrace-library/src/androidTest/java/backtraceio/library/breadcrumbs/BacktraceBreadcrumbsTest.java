package backtraceio.library.breadcrumbs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

@RunWith(AndroidJUnit4.class)
public class BacktraceBreadcrumbsTest {
    public Context context;

    static
    {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() throws IOException {
        this.context = InstrumentationRegistry.getContext();
    }

    @After
    public void cleanUp() {
        File dir = new File(context.getFilesDir().getAbsolutePath() + "/breadcrumbs");
        deleteRecursive(dir);
    }

    @Test
    public void testEnable() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);

            backtraceBreadcrumbs.enableBreadcrumbs(context);
            assertTrue(backtraceBreadcrumbs.isBreadcrumbsEnabled());

            backtraceBreadcrumbs.disableBreadcrumbs(context);
            assertFalse(backtraceBreadcrumbs.isBreadcrumbsEnabled());

        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumb() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(context);

            backtraceBreadcrumbs.addBreadcrumb("Test");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumbWithAttributes() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(context);

            Map<String, Object> attributes = new HashMap<String, Object>() {{
               put("floopy","doopy");
               put("flim","flam");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("doopy", parsedBreadcrumb.get("floopy"));
            assertEquals("flam", parsedBreadcrumb.get("flim"));

        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    // We should preserve spaces in the message string
    @Test
    public void testSpaceInMessage() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs(context);

            backtraceBreadcrumbs.addBreadcrumb("Testing 1 2 3");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));

        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    public void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    // This assumes we will only have one breadcrumb log file, which we will most of the time
    public List<String> readBreadcrumbLogFiles(BacktraceBreadcrumbs backtraceBreadcrumbs) throws FileNotFoundException {
        File breadcrumbLogFilesDir = new File(backtraceBreadcrumbs.getBreadcrumbLogDirectory());
        File[] breadcrumbLogFiles = breadcrumbLogFilesDir.listFiles();

        List<String> breadcrumbLogFileData = new ArrayList<String>();

        Scanner scan = new Scanner(new File(breadcrumbLogFiles[0].getAbsolutePath()));

        while(scan.hasNextLine()) {
            breadcrumbLogFileData.add(scan.nextLine());
        }

        return breadcrumbLogFileData;
    }

    // THIS IS NOT A FULLY ROBUST BREADCRUMB PARSER. THIS IS JUST FOR TESTING
    public Map<String, String> parseBreadcrumb(String breadcrumb) {
        Map<String, String> parsedBreadcrumb = new HashMap<String, String>();

        String[] breadcrumbTokens = breadcrumb.split("[ ]+");

        for (int i = 0; i < breadcrumbTokens.length; i++) {
            switch (breadcrumbTokens[i]) {
                case "attributes":
                    while (breadcrumbTokens[++i].equals("attr"))
                    {
                        parsedBreadcrumb.put(breadcrumbTokens[++i], breadcrumbTokens[++i]);
                    }
                case "message":
                    String reconstructedMessage = "";
                    for (i++ ; i < breadcrumbTokens.length; i++) {
                        reconstructedMessage += breadcrumbTokens[i];
                        if (i < breadcrumbTokens.length - 1) {
                            reconstructedMessage += " ";
                        }
                    }
                    parsedBreadcrumb.put("message", reconstructedMessage);
                    break;
                default:
                    parsedBreadcrumb.put(breadcrumbTokens[i++], breadcrumbTokens[i]);
            }
        }

        return parsedBreadcrumb;
    }
}
