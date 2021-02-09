package backtraceio.library.breadcrumbs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
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

            backtraceBreadcrumbs.enableBreadcrumbs();
            assertTrue(backtraceBreadcrumbs.isBreadcrumbsEnabled());

            backtraceBreadcrumbs.disableBreadcrumbs();
            assertFalse(backtraceBreadcrumbs.isBreadcrumbsEnabled());

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumb() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            // Also account for the encoding (newlines before and after the breadcrumb string)
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumbWithAttributes() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            Map<String, Object> attributes = new HashMap<String, Object>() {{
               put("floopy","doopy");
               put("flim","flam");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            // Also account for the encoding (newlines before and after the breadcrumb string)
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("doopy", parsedBreadcrumb.get("floopy"));
            assertEquals("flam", parsedBreadcrumb.get("flim"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should preserve spaces in the message string
    @Test
    public void testSpaceInMessage() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            backtraceBreadcrumbs.addBreadcrumb("Testing 1 2 3");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            // Also account for the encoding (newlines before and after the breadcrumb string)
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should remove \n in the message string
    @Test
    public void testNewlineInMessage() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            backtraceBreadcrumbs.addBreadcrumb("Testing\n 1 2\n 3\n");

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            // Also account for the encoding (newlines before and after the breadcrumb string)
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should NOT preserve spaces or newlines for any non-message field
    @Test
    public void testInvalidCharsInAttribute() {

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            Map<String, Object> attributes = new HashMap<String, Object>() {{
               put(" flo opy","do o py ");
               put("fl\nim","fl\nam\n");
               put(" foo ","b\na r ");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            // Also account for the encoding (newlines before and after the breadcrumb string)
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("do_o_py_", parsedBreadcrumb.get("_flo_opy"));
            assertEquals("flam", parsedBreadcrumb.get("flim"));
            assertEquals("ba_r_", parsedBreadcrumb.get("_foo_"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void breadcrumbsEnduranceTest() {
        int numIterationsPerThread = 100;
        int numThreads = 4;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numThreads; i++) {
                new Thread(new BreadcrumbLogger(backtraceBreadcrumbs, numIterationsPerThread)).start();
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles(backtraceBreadcrumbs);

            List<Map<String, String>> parsedBreadcrumbList = new ArrayList<Map<String, String>>();
            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++)
            {
                Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(i));
                parsedBreadcrumbList.add(parsedBreadcrumb);
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.get("From_Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a number
                Long.parseLong(parsedBreadcrumb.get("timestamp"));
                // Id should be convertible to a number
                Integer.parseInt(parsedBreadcrumb.get("id"));
            }

            Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
                public int compare(Map<String, String> m1, Map<String, String> m2) {
                    return Integer.parseInt(m1.get("id")) - Integer.parseInt(m2.get("id"));
                }
            };

            Collections.sort(parsedBreadcrumbList, mapComparator);
            // In this case we expect breadcrumbs with a greater id to never have a
            // timestamp less than a breadcrumb with a lesser id
            // It is only possible if the user changes the android system time during runtime
            for (int i = 0; i < parsedBreadcrumbList.size() - 1; i++)
            {
                assertTrue(Long.parseLong(parsedBreadcrumbList.get(i+1).get("timestamp")) >=
                            Long.parseLong(parsedBreadcrumbList.get(i).get("timestamp")));
            }

        } catch(Exception ex) {
            fail(ex.getMessage());
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

    class BreadcrumbLogger implements Runnable {
        BacktraceBreadcrumbs backtraceBreadcrumbs;
        int numIterations;

        BreadcrumbLogger(BacktraceBreadcrumbs backtraceBreadcrumbs, int numIterations) {
            this.backtraceBreadcrumbs = backtraceBreadcrumbs;
            this.numIterations = numIterations;
        }

        @Override
        public void run() {
            for (int i = 0; i < this.numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};

                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }
        }
    }

    // This assumes we will only have one breadcrumb log file, which we will most of the time
    public List<String> readBreadcrumbLogFiles(BacktraceBreadcrumbs backtraceBreadcrumbs) throws IOException {
        File breadcrumbLogFilesDir = new File(backtraceBreadcrumbs.getBreadcrumbLogDirectory());
        File[] breadcrumbLogFiles = breadcrumbLogFilesDir.listFiles();

        List<String> breadcrumbLogFileData = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(breadcrumbLogFiles[0].getAbsolutePath());

        // The encoding contains headers for the encoded data
        // We just throw away lines that don't start with "timestamp
        StringBuilder stringBuilder = new StringBuilder();
        while(inputStream.available() > 0) {
            char c = (char) inputStream.read();
            if (c == '\n')
            {
                String line = stringBuilder.toString();
                if (line.matches(".*timestamp.*"))
                {
                    breadcrumbLogFileData.add(line);
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder.append(c);
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
