package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.common.FileHelper;
import backtraceio.library.common.MultiFormRequestHelper;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceFileAttachments {
    private BacktraceCredentials credentials;
    private BacktraceClient client;
    private static final String fileName = "test.txt";
    private static final String fileContent = "file content";
    private static Context context;
    private String absolutePath;


    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        absolutePath = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        credentials = new BacktraceCredentials("", "");
        client = new BacktraceClient(context, credentials);
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android
            .Manifest.permission.READ_EXTERNAL_STORAGE);

    @Test
    public void streamFileAttachment() {
        // GIVEN
        final Waiter waiter = new Waiter();
        createTestFile();
        final List<String> attachments = new ArrayList<String>() {{
            add(absolutePath);
        }};

        // WHEN
        final List<byte[]> fileContents = new ArrayList<>();
        client.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    for (String path : attachments) {
                        MultiFormRequestHelper.streamFile(bos, path);
                        fileContents.add(bos.toByteArray());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return new BacktraceResult();
            }
        });
        client.send(new BacktraceReport("test", null, attachments), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertNotNull(backtraceResult);
                        assertEquals(backtraceResult.status, BacktraceResultStatus.Ok);
                        assertEquals(1, fileContents.size());
                        assertEquals(fileContent, new String(fileContents.get(0)));
                        waiter.resume();
                    }
                });
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void streamNotExistFiles() {
        // GIVEN
        final Waiter waiter = new Waiter();
        final List<String> attachments = new ArrayList<String>() {{
            add(null);
            add("");
            add("broken path");
            add(context.getFilesDir().getAbsolutePath() + "/not_existing_file.txt");
        }};

        // WHEN
        final List<byte[]> fileContents = new ArrayList<>();
        final List<String> filteredAttachments = FileHelper.filterOutFiles(context, attachments);
        client.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                try {

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    MultiFormRequestHelper.addFiles(bos, filteredAttachments);
                    if (bos.size() != 0) {
                        fileContents.add(bos.toByteArray());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return new BacktraceResult();
            }
        });
        client.send(new BacktraceReport("test", null, attachments), new
                OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        // THEN
                        assertNotNull(backtraceResult);
                        assertEquals(backtraceResult.status, BacktraceResultStatus.Ok);
                        assertEquals(0, fileContents.size());
                        waiter.resume();
                    }
                });
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(1005, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @After
    public void tearDown() {
        deleteTestFile();
    }


    public boolean createTestFile() {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            Writer out = new OutputStreamWriter(fos);
            out.write(fileContent);
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTestFile() {
        File file = new File(absolutePath);
        return file.delete();
    }
}
