package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientTest {
    private final String resultMessage = "From request handler";

    private Context context;
    private BacktraceCredentials credentials;
    private final List<String> initAttachments = new ArrayList<String>() {{
        add("file1.txt");
        add("example/file2.txt");
    }};

    private final List<String> dynamicAttachments = new ArrayList<String>() {{
        add("file3.txt");
        add("example2/file4.txt");
    }};

    private final List<String> reportAttachments = new ArrayList<String>() {{
        add("file5.txt");
        add("example3/file6.txt");
    }};

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    private void sendAndAssert(
            BacktraceReport report,
            List<String> initAttachments,
            List<String> dynamicAttachments,
            Consumer<BacktraceData> assertFn) {

        BacktraceClient client = new BacktraceClient(context, credentials, initAttachments);

        for (String attachment : dynamicAttachments) {
            client.addAttachment(attachment);
        }

        final Waiter waiter = new Waiter();

        client.setOnRequestHandler(data -> {
            assertFn.accept(data);
            waiter.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        });

        client.send(report);
        try {
            waiter.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceReportWithoutAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage),
                new ArrayList<>(),
                new ArrayList<>(),
                data ->
                        assertEquals(0, data.getAttachmentPaths().size())
        );
    }

    @Test
    public void sendBacktraceReportWithInitAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage),
                initAttachments,
                new ArrayList<>(),
                data ->
                        assertEquals(initAttachments.size(), data.getAttachmentPaths().size())
        );
    }

    @Test
    public void sendBacktraceReportWithDynamicAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage),
                new ArrayList<>(),
                dynamicAttachments,
                data ->
                        assertEquals(dynamicAttachments.size(), data.getAttachmentPaths().size())
        );
    }

    @Test
    public void sendBacktraceReportWithReportAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage, reportAttachments),
                new ArrayList<>(),
                new ArrayList<>(),
                data ->
                        assertEquals(reportAttachments.size(), data.getAttachmentPaths().size())
        );
    }

    @Test
    public void sendBacktraceReportWithInitAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage, reportAttachments),
                initAttachments,
                new ArrayList<>(),
                data ->
                        assertEquals(reportAttachments.size() + initAttachments.size(), data.getAttachmentPaths().size())
        );
    }

    @Test
    public void sendBacktraceReportWithDynamicAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage, reportAttachments),
                new ArrayList<>(),
                dynamicAttachments,
                data -> {
                    assertEquals(reportAttachments.size() + dynamicAttachments.size(), data.getAttachmentPaths().size());
//                    assertEquals("file3.txt", data.getAttachmentPaths().get(0));
//                    assertEquals("example2/file4.txt", data.getAttachmentPaths().get(1));
//                    assertEquals("file1.txt", data.getAttachmentPaths().get(2));
//                    assertEquals("example/file2.txt", data.getAttachmentPaths().get(3));
                }
        );
    }

    @Test
    public void sendBacktraceReportWithInitAndDynamicAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage),
                initAttachments,
                dynamicAttachments,
                data -> {
                    List<String> attachments = data.getAttachmentPaths();
                    assertEquals(initAttachments.size() + dynamicAttachments.size(), attachments.size());
//                    assertEquals("file1.txt", attachments.get(0));
//                    assertEquals("example/file2.txt", attachments.get(1));
//                    assertEquals("file3.txt", attachments.get(2));
//                    assertEquals("example2/file4.txt", attachments.get(3));
                }
        );
    }

    @Test
    public void sendBacktraceReportWithInitDynamicAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(this.resultMessage, reportAttachments),
                initAttachments,
                dynamicAttachments,
                data -> {
                    assertEquals(reportAttachments.size() + initAttachments.size() + dynamicAttachments.size(), data.getAttachmentPaths().size());
//                    assertEquals("file3.txt", data.getAttachmentPaths().get(0));
//                    assertEquals("example2/file4.txt", data.getAttachmentPaths().get(1));
//                    assertEquals("file1.txt", data.getAttachmentPaths().get(2));
//                    assertEquals("example/file2.txt", data.getAttachmentPaths().get(3));
//                    assertEquals("file5.txt", data.getAttachmentPaths().get(2));
//                    assertEquals("example3/file6.txt", data.getAttachmentPaths().get(3));
                }
        );
    }

    @Test
    public void sendBacktraceReportWithDynamicAttributesModifyDuringExecution() {
        // SEND 1-st report
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        final Waiter waiter = new Waiter();

        RequestHandler rh = data -> {
            assertEquals(0, data.getAttachmentPaths().size());
            waiter.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        };

        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage));
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        // SEND 2-nd report
        Waiter waiter2 = new Waiter();

        backtraceClient.addAttachment("test-attachment.txt");

        rh = data -> {
            assertEquals(1, data.getAttachmentPaths().size());
            waiter2.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage));
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter2.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void sendBacktraceReportWithDynamicAttributesRemovedDuringExecution() {
        // SEND 1-st report
        // GIVEN
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials);
        backtraceClient.addAttachment("file.txt");
        backtraceClient.addAttachment("file2.txt");

        final Waiter waiter = new Waiter();

        RequestHandler rh = data -> {
            assertEquals(2, data.getAttachmentPaths().size());
            waiter.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        };

        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage));
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        // SEND 2-nd report
        Waiter waiter2 = new Waiter();
        backtraceClient.getAttachments().remove("file.txt");

        rh = data -> {
            assertEquals(1, data.getAttachmentPaths().size());
            assertEquals("file2.txt", data.getAttachmentPaths().get(0));
            waiter2.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        };
        backtraceClient.setOnRequestHandler(rh);

        // WHEN
        backtraceClient.send(new BacktraceReport(this.resultMessage));
        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter2.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
