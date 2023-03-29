package backtraceio.library.breadcrumbs;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RunWith(AndroidJUnit4.class)
public class BacktraceBreadcrumbsTest {
    public Context context;
    public BacktraceBreadcrumbs backtraceBreadcrumbs;
    public String absolutePath;

    static {
        System.loadLibrary("backtrace-native");
    }

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        backtraceBreadcrumbs = new BacktraceBreadcrumbs(context.getFilesDir().getAbsolutePath());
        backtraceBreadcrumbs.enableBreadcrumbs(context);
    }

    @After
    public void cleanUp() {
        File dir = new File(context.getFilesDir().getAbsolutePath());
        deleteRecursive(dir);
    }

    @Test
    public void testAddBreadcrumb() {
        try {
            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testClearBreadcrumbs() {
        try {
            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());
            assertEquals(2, breadcrumbLogFileData.size());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

            assertTrue(backtraceBreadcrumbs.clearBreadcrumbs());

            // Should have cleared the breadcrumb we just read but
            // We should still have a configuration breadcrumb
            breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());
            assertEquals(1, breadcrumbLogFileData.size());
            parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));

            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testEnableBreadcrumbs() {
        try {
            cleanUp();

            backtraceBreadcrumbs = new BacktraceBreadcrumbs(context.getFilesDir().getAbsolutePath());
            assertTrue(backtraceBreadcrumbs.enableBreadcrumbs(context));

            assertTrue(backtraceBreadcrumbs.addBreadcrumb("Test"));

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testAddBreadcrumbWithAttributes() {
        try {
            Map<String, Object> attributes = new HashMap<String, Object>() {{
                put("floopy", "doopy");
                put("flim", "flam");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("doopy", parsedBreadcrumb.getJSONObject("attributes").get("floopy"));
            assertEquals("flam", parsedBreadcrumb.getJSONObject("attributes").get("flim"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should preserve spaces in the message string
    @Test
    public void testSpaceInMessage() {
        try {
            backtraceBreadcrumbs.addBreadcrumb("Testing 1 2 3");

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should remove \n in the message string
    @Test
    public void testNewlineInMessage() {
        try {
            backtraceBreadcrumbs.addBreadcrumb("Testing\n 1 2\n 3\n");

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Testing 1 2 3", parsedBreadcrumb.get("message"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    // We should NOT preserve spaces or newlines for any non-message field
    @Test
    public void testInvalidCharsInAttribute() {
        try {
            Map<String, Object> attributes = new HashMap<String, Object>() {{
                put(" flo opy", "do o py ");
                put("fl\nim", "fl\nam\n");
                put(" foo ", "b\na r ");
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals("do o py ", parsedBreadcrumb.getJSONObject("attributes").get(" flo opy"));
            assertEquals("flam", parsedBreadcrumb.getJSONObject("attributes").get("flim"));
            assertEquals("ba r ", parsedBreadcrumb.getJSONObject("attributes").get(" foo "));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongMessage() {
        try {
            backtraceBreadcrumbs.addBreadcrumb(longTestMessage);

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals(expectedLongTestMessage, parsedBreadcrumb.get("message"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesLongFirst() {
        try {
            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(longTestAttributeKey, longTestAttributeValue);
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(expectedLongTestAttributeValue, parsedBreadcrumb.getJSONObject("attributes").get(expectedLongTestAttributeKey));
            assertFalse(parsedBreadcrumb.getJSONObject("attributes").has(reasonableLengthAttributeKey));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLongAttributesShortFirst() {
        try {
            final Map<String, Object> attributes = new LinkedHashMap<String, Object>() {{
                put(reasonableLengthAttributeKey, reasonableLengthAttributeValue);
                put(longTestAttributeKey, longTestAttributeValue);
            }};

            backtraceBreadcrumbs.addBreadcrumb("Test", attributes);

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(1));

            assertEquals("Test", parsedBreadcrumb.get("message"));
            assertEquals(reasonableLengthAttributeValue, parsedBreadcrumb.getJSONObject("attributes").get(reasonableLengthAttributeKey));
            assertFalse(parsedBreadcrumb.getJSONObject("attributes").has(expectedLongTestAttributeKey));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRollover() {
        int numIterations = 400;
        // Account for mandatory configuration breadcrumb
        backtraceBreadcrumbs.setCurrentBreadcrumbId(1);

        try {
            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb, it should be valid
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                assertTrue(parsedBreadcrumb.get("id") instanceof Integer);
            }

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRollover() {
        // Should reach the max of 64kB around breadcrumb 925
        final int numIterations = 1000;
        // Account for mandatory configuration breadcrumb
        backtraceBreadcrumbs.setCurrentBreadcrumbId(1);

        try {
            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            long breadcrumbsFileSize = new File(backtraceBreadcrumbs.getBreadcrumbLogPath()).length();
            assertTrue(String.format("Size of breadcrumbs file (%s) not close enough to a full breadcrumb file (%s)", breadcrumbsFileSize, 64 * 1024),
                    breadcrumbsFileSize > 63 * 1024);

            // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());
            for (int i = 0; i < breadcrumbLogFileData.size(); i++) {
                JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                final int id = (int) parsedBreadcrumb.get("id");
                assertTrue(String.format("Breadcrumb ID %s was higher than the expected numIterations %s",
                        id, numIterations), id <= numIterations);
            }

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileShouldNotRolloverCustomMax() {
        int numIterations = 40;
        // Cleanup after default BacktraceBreadcrumbs constructor
        // Because we want to create our own instance with custom parameters
        cleanUp();

        try {
            backtraceBreadcrumbs = new BacktraceBreadcrumbs(context.getFilesDir().getAbsolutePath());
            backtraceBreadcrumbs.enableBreadcrumbs(context, 6400);
            // Account for mandatory configuration breadcrumb
            backtraceBreadcrumbs.setCurrentBreadcrumbId(1);

            for (int i = 0; i < numIterations; i++) {
                final long threadId = Thread.currentThread().getId();
                Map<String, Object> attributes = new HashMap<String, Object>() {{
                    put("From Thread", threadId);
                }};
                backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
            }

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb, it should be valid
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(0));
            assertEquals("Breadcrumbs configuration", parsedBreadcrumb.get("message"));

            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                assertTrue(parsedBreadcrumb.get("id") instanceof Integer);
            }

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testQueueFileRolloverCustomMax() throws IOException, JSONException {
        int numIterations = 100;
        // Cleanup after default BacktraceBreadcrumbs constructor
        // Because we want to create our own instance with custom parameters
        cleanUp();

        backtraceBreadcrumbs = new BacktraceBreadcrumbs(context.getFilesDir().getAbsolutePath());
        backtraceBreadcrumbs.enableBreadcrumbs(context, 6400);
        // Account for mandatory configuration breadcrumb
        backtraceBreadcrumbs.setCurrentBreadcrumbId(1);

        for (int i = 0; i < numIterations; i++) {
            final long threadId = Thread.currentThread().getId();
            Map<String, Object> attributes = new HashMap<String, Object>() {{
                put("From Thread", threadId);
            }};
            backtraceBreadcrumbs.addBreadcrumb("I am a breadcrumb", attributes);
        }

        List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

        // We should have rolled over the configuration breadcrumb, consider all breadcrumbs here
        for (int i = 0; i < breadcrumbLogFileData.size(); i++) {
            JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
            assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
            assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
            assertEquals("manual", parsedBreadcrumb.get("type"));
            assertEquals("info", parsedBreadcrumb.get("level"));
            // Timestamp should be convertible to a long
            assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
            assertTrue(((int) parsedBreadcrumb.get("id")) > 45);
        }
    }

    @Test
    public void breadcrumbsEnduranceTest() {
        int numIterationsPerThread = 200;
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        try {
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(new BreadcrumbLogger(backtraceBreadcrumbs, numIterationsPerThread));
                threads[i].start();
            }
            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            List<String> breadcrumbLogFileData = BreadcrumbsReader.readBreadcrumbLogFile(context.getFilesDir().getAbsolutePath());

            // First breadcrumb is configuration breadcrumb
            // We start from the second breadcrumb
            for (int i = 1; i < breadcrumbLogFileData.size(); i++) {
                JSONObject parsedBreadcrumb = new JSONObject(breadcrumbLogFileData.get(i));
                assertEquals("I am a breadcrumb", parsedBreadcrumb.get("message"));
                assertNotNull(parsedBreadcrumb.getJSONObject("attributes").get("From Thread"));
                assertEquals("manual", parsedBreadcrumb.get("type"));
                assertEquals("info", parsedBreadcrumb.get("level"));
                // Timestamp should be convertible to a long
                assertTrue(parsedBreadcrumb.get("timestamp") instanceof Long);
                assertTrue(parsedBreadcrumb.get("id") instanceof Long);
            }

        } catch (Exception ex) {
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

    

    private final String longTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.\n" +
            "\n" +
            "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra diam sit amet. Ipsum dolor sit amet consectetur adipiscing elit duis. Adipiscing vitae proin sagittis nisl rhoncus mattis rhoncus. Faucibus interdum posuere lorem ipsum dolor. Aliquet risus feugiat in ante metus dictum at. Pretium aenean pharetra magna ac placerat vestibulum lectus mauris ultrices. Enim nulla aliquet porttitor lacus luctus accumsan. Diam ut venenatis tellus in metus. Facilisi nullam vehicula ipsum a arcu cursus.\n" +
            "\n" +
            "Sed faucibus turpis in eu mi bibendum neque egestas congue. Ipsum nunc aliquet bibendum enim facilisis gravida neque convallis. Vitae congue mauris rhoncus aenean vel elit scelerisque mauris pellentesque. Id donec ultrices tincidunt arcu non sodales neque. Eu turpis egestas pretium aenean pharetra magna ac. Est ullamcorper eget nulla facilisi etiam dignissim diam. Eget arcu dictum varius duis at. Pretium quam vulputate dignissim suspendisse in est. Morbi quis commodo odio aenean sed adipiscing diam. Leo urna molestie at elementum eu.";

    private final String expectedLongTestMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio ut sem nulla pharetra dia";

    private final String longTestAttributeKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.";

    private final String longTestAttributeValue = "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio";

    private final String expectedLongTestAttributeKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ipsum consequat nisl vel pretium lectus quam id. Velit dignissim sodales ut eu sem integer vitae justo. Cursus euismod quis viverra nibh cras pulvinar. Pellentesque adipiscing commodo elit at imperdiet. Pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum. Elementum facilisis leo vel fringilla est ullamcorper eget nulla. Purus sit amet luctus venenatis. Non consectetur a erat nam at. Pellentesque id nibh tortor id aliquet lectus proin. Purus semper eget duis at tellus. Sodales ut etiam sit amet nisl purus. Viverra justo nec ultrices dui sapien eget.";

    private final String expectedLongTestAttributeValue = "Et ultrices neque ornare aenean euismod elementum nisi quis eleifend. Ut diam quam nulla porttitor. Vitae elementum curabitur vitae nunc sed. Feugiat sed lectus vestibulum mattis ullamcorper velit sed. A diam sollicitudin tempor id eu nisl nunc. At urna condimentum mattis pellentesque id. Arcu odio";

    private final String reasonableLengthAttributeKey = "reasonablySizedKey";

    private final String reasonableLengthAttributeValue = "reasonableSizedValue";
}
