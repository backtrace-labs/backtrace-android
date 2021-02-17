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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
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

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
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

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
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

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
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

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Testing_ 1 2_ 3_", parsedBreadcrumb.get("message"));

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

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("do_o_py_", parsedBreadcrumb.get("_flo_opy"));
            assertEquals("fl_am_", parsedBreadcrumb.get("fl_im"));
            assertEquals("b_a_r_", parsedBreadcrumb.get("_foo_"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongMessage() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            backtraceBreadcrumbs.addBreadcrumb(longTestMessage);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals(expectedLongTestMessage, parsedBreadcrumb.get("message"));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesLongFirst() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(longTestAttributeKey, longTestAttributeValue);
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(expectedLongTestAttributeValue, parsedBreadcrumb.get(expectedLongTestAttributeKey));
            assertNull(parsedBreadcrumb.get(reasonableLengthAttributeKey));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesShortFirst() {
        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
                put(longTestAttributeKey, longTestAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(reasonableLengthAttributeValue, parsedBreadcrumb.get(reasonableLengthAttributeKey));
            assertNull(parsedBreadcrumb.get(expectedLongTestAttributeKey));

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRollover() {
        int numIterations = 500;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            List<Map<String, String>> parsedBreadcrumbList = new ArrayList<Map<String, String>>();

            // First breadcrumb is configuration breadcrumb, it should be valid
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(0));
            parsedBreadcrumbList.add(parsedBreadcrumb);
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));
            assertTrue(Integer.parseInt(parsedBreadcrumb.get("id")) == 0);


            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++)
            {
                parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(i));
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

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRollover() {
        int numIterations = 1000;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            List<Map<String, String>> parsedBreadcrumbList = new ArrayList<Map<String, String>>();
            // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
            for (int i = 0; i < breadcrumbLogFileData.size(); i++)
            {
                Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(i));
                parsedBreadcrumbList.add(parsedBreadcrumb);
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.get("From_Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a number
                Long.parseLong(parsedBreadcrumb.get("timestamp"));
                // Id should be convertible to a number, and should skew larger (earlier breadcrumbs rolled off)
                assertTrue(Integer.parseInt(parsedBreadcrumb.get("id")) > 450);
            }

            Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
                public int compare(Map<String, String> m1, Map<String, String> m2) {
                    return Integer.parseInt(m1.get("id")) - Integer.parseInt(m2.get("id"));
                }
            };

            Collections.sort(parsedBreadcrumbList, mapComparator);

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRolloverCustomMax() {
        int numIterations = 50;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context, 6400);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            List<Map<String, String>> parsedBreadcrumbList = new ArrayList<Map<String, String>>();

            // First breadcrumb is configuration breadcrumb, it should be valid
            Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(0));
            parsedBreadcrumbList.add(parsedBreadcrumb);
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));
            assertTrue(Integer.parseInt(parsedBreadcrumb.get("id")) == 0);


            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++)
            {
                parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(i));
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

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRolloverCustomMax() {
        int numIterations = 100;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context, 6400);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

            List<Map<String, String>> parsedBreadcrumbList = new ArrayList<Map<String, String>>();
            // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
            for (int i = 0; i < breadcrumbLogFileData.size(); i++)
            {
                Map<String, String> parsedBreadcrumb = parseBreadcrumb(breadcrumbLogFileData.get(i));
                parsedBreadcrumbList.add(parsedBreadcrumb);
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.get("From_Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a number
                Long.parseLong(parsedBreadcrumb.get("timestamp"));
                // Id should be convertible to a number, and should skew larger (earlier breadcrumbs rolled off)
                assertTrue(Integer.parseInt(parsedBreadcrumb.get("id")) > 45);
            }

            Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
                public int compare(Map<String, String> m1, Map<String, String> m2) {
                    return Integer.parseInt(m1.get("id")) - Integer.parseInt(m2.get("id"));
                }
            };

            Collections.sort(parsedBreadcrumbList, mapComparator);

        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void breadcrumbsEnduranceTest() {
        int numIterationsPerThread = 200;
        int numThreads = 10;

        try {
            BacktraceBreadcrumbs backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();

            for (int i = 0; i < numThreads; i++) {
                new Thread(new BreadcrumbLogger(backtraceBreadcrumbs, numIterationsPerThread)).start();
            }

            List<String> breadcrumbLogFileData = readBreadcrumbLogFiles();

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
    public List<String> readBreadcrumbLogFiles() throws IOException {
        File breadcrumbLogFilesDir = new File(BacktraceBreadcrumbs.getBreadcrumbLogDirectory(context));
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

    private final String longTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.\n" +
            "\n" +
            "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra diam sit amet. Ipsum dolor sit amet consectetur adipiscing elit duis. Adipiscing vitae proin sagittis nisl rhoncus mattis rhoncus. Faucibus interdum posuere lorem ipsum dolor. Aliquet risus feugiat in ante metus dictum at. Pretium aenean pharetra magna ac placerat vestibulum lectus mauris ultrices. Enim nulla aliquet porttitor lacus luctus accumsan. Diam ut venenatis tellus in metus. Facilisi nullam vehicula ipsum a arcu cursus.\n" +
            "\n" +
            "Sed faucibus turpis in eu mi bibendum neque egestas congue. Ipsum nunc aliquet bibendum enim facilisis gravida neque convallis. Vitae congue mauris rhoncus aenean vel elit scelerisque mauris pellentesque. Id donec ultrices tincidunt arcu non sodales neque. Eu turpis egestas pretium aenean pharetra magna ac. Est ullamcorper eget nulla facilisi etiam dignissim diam. Eget arcu dictum varius duis at. Pretium quam vulputate dignissim suspendisse in est. Morbi quis commodo odio aenean sed adipiscing diam. Leo urna molestie at elementum eu."
            ;

    private final String expectedLongTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.__Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra ..."
            ;

    private final String longTestAttributeKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.";

    private final String longTestAttributeValue = "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio";

    private final String expectedLongTestAttributeKey = "Lorem_ipsum_dolor_sit_amet,_consectetur_adipiscing_elit,_sed_do_eiusmod_tempor_incididunt_ut_labore_et_dolore_magna_aliqua._Ipsum_consequat_nisl_vel_pretium_lectus_quam_id._Velit_dignissim_sodales_ut_eu_sem_integer_vitae_justo._Cursus_euismod_quis_viverra_nibh_cras_pulvinar._Pellentesque_adipiscing_commodo_elit_at_imperdiet._Pellentesque_eu_tincidunt_tortor_aliquam_nulla_facilisi_cras_fermentum._Elementum_facilisis_leo_vel_fringilla_est_ullamcorper_eget_nulla._Purus_sit_amet_luctus_venenatis._Non_consectetur_a_erat_nam_at._Pellentesque_id_nibh_tortor_id_aliquet_lectus_proin._Purus_semper_eget_duis_at_tellus._Sodales_ut_etiam_sit_amet_nisl_purus._Viverra_justo_nec_ultrices_dui_sapien_eget.";

    private final String expectedLongTestAttributeValue = "Et_ultrices_neque_ornare_aenean_euismod_elementum_nisi_quis_eleifend._Ut_diam_quam_nulla_porttitor._Vitae_elementum_curabitur_vitae_nunc_sed._Feugiat_sed_lectus_vestibulum_mattis_ullamcorper_velit_sed._A_diam_sollicitudin_tempor_id_eu_nisl_nunc._At_urna_condimentum_mattis_pellentesque_id._Arcu_odio";

    private final String reasonableLengthAttributeKey = "reasonablySizedKey";

    private final String reasonableLengthAttributeValue = "reasonableSizedValue";
}
