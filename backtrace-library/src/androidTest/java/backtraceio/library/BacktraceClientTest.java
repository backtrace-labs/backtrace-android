package backtraceio.library;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.jodah.concurrentunit.Waiter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceClientTest {
    private static final String RESULT_MESSAGE = "From request handler";
    private static final int WAIT_TIMEOUT_SECONDS = 3;

    private Context context;
    private BacktraceCredentials credentials;
    private final List<String> initAttachments = Arrays.asList("file1.txt", "example/file2.txt");
    private final List<String> dynamicAttachments = Arrays.asList("file3.txt", "example2/file4.txt");
    private final List<String> reportAttachments = Arrays.asList("file5.txt", "example3/file6.txt");

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

        sendReportAndWait(client, report, assertFn);
    }

    private void sendReportAndWait(BacktraceClient client, BacktraceReport report, Consumer<BacktraceData> assertFn) {
        Waiter waiter = new Waiter();

        client.setOnRequestHandler(data -> {
            assertFn.accept(data);
            waiter.resume();
            return createSuccessResult(data);
        });

        client.send(report);
        awaitWaiter(waiter);
    }

    private BacktraceResult createSuccessResult(BacktraceData data) {
        return new BacktraceResult(data.getReport(), data.getReport().message, BacktraceResultStatus.Ok);
    }

    private void awaitWaiter(Waiter waiter) {
        try {
            waiter.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    private void assertAttachmentsContain(BacktraceData data, List<String> expectedAttachments) {
        List<String> actualAttachments = data.getAttachmentPaths();
        for (String expected : expectedAttachments) {
            assertTrue(
                    "Expected attachment '" + expected + "' not found in " + actualAttachments,
                    actualAttachments.contains(expected));
        }
    }

    @Test
    public void sendBacktraceReportWithoutAttachments() {
        sendAndAssert(
                new BacktraceReport(RESULT_MESSAGE),
                new ArrayList<>(),
                new ArrayList<>(),
                data -> assertEquals(0, data.getAttachmentPaths().size()));
    }

    @Test
    public void sendBacktraceReportWithInitAttachments() {
        sendAndAssert(new BacktraceReport(RESULT_MESSAGE), initAttachments, new ArrayList<>(), data -> {
            assertEquals(initAttachments.size(), data.getAttachmentPaths().size());
            assertAttachmentsContain(data, initAttachments);
        });
    }

    @Test
    public void sendBacktraceReportWithDynamicAttachments() {
        sendAndAssert(new BacktraceReport(RESULT_MESSAGE), new ArrayList<>(), dynamicAttachments, data -> {
            assertEquals(dynamicAttachments.size(), data.getAttachmentPaths().size());
            assertAttachmentsContain(data, dynamicAttachments);
        });
    }

    @Test
    public void sendBacktraceReportWithReportAttachments() {
        sendAndAssert(
                new BacktraceReport(RESULT_MESSAGE, reportAttachments), new ArrayList<>(), new ArrayList<>(), data -> {
                    assertEquals(
                            reportAttachments.size(), data.getAttachmentPaths().size());
                    assertAttachmentsContain(data, reportAttachments);
                });
    }

    @Test
    public void sendBacktraceReportWithInitAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(RESULT_MESSAGE, reportAttachments), initAttachments, new ArrayList<>(), data -> {
                    assertEquals(
                            reportAttachments.size() + initAttachments.size(),
                            data.getAttachmentPaths().size());
                    assertAttachmentsContain(data, reportAttachments);
                    assertAttachmentsContain(data, initAttachments);
                });
    }

    @Test
    public void sendBacktraceReportWithDynamicAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(RESULT_MESSAGE, reportAttachments), new ArrayList<>(), dynamicAttachments, data -> {
                    assertEquals(
                            reportAttachments.size() + dynamicAttachments.size(),
                            data.getAttachmentPaths().size());
                    assertAttachmentsContain(data, reportAttachments);
                    assertAttachmentsContain(data, dynamicAttachments);
                });
    }

    @Test
    public void sendBacktraceReportWithInitAndDynamicAttachments() {
        sendAndAssert(new BacktraceReport(RESULT_MESSAGE), initAttachments, dynamicAttachments, data -> {
            assertEquals(
                    initAttachments.size() + dynamicAttachments.size(),
                    data.getAttachmentPaths().size());
            assertAttachmentsContain(data, initAttachments);
            assertAttachmentsContain(data, dynamicAttachments);
        });
    }

    @Test
    public void sendBacktraceReportWithInitDynamicAndReportAttachments() {
        sendAndAssert(
                new BacktraceReport(RESULT_MESSAGE, reportAttachments), initAttachments, dynamicAttachments, data -> {
                    assertEquals(
                            reportAttachments.size() + initAttachments.size() + dynamicAttachments.size(),
                            data.getAttachmentPaths().size());
                    assertAttachmentsContain(data, reportAttachments);
                    assertAttachmentsContain(data, initAttachments);
                    assertAttachmentsContain(data, dynamicAttachments);
                });
    }

    @Test
    public void sendBacktraceReportWithDynamicAttributesModifyDuringExecution() {
        BacktraceClient client = new BacktraceClient(context, credentials);

        // Send first report with no attachments
        sendReportAndWait(client, new BacktraceReport(RESULT_MESSAGE), data -> {
            assertEquals(0, data.getAttachmentPaths().size());
        });

        // Add attachment and send second report
        String attachment = "test-attachment.txt";
        client.addAttachment(attachment);
        sendReportAndWait(client, new BacktraceReport(RESULT_MESSAGE), data -> {
            assertEquals(1, data.getAttachmentPaths().size());
            assertTrue(
                    "Expected attachment '" + attachment + "' not found",
                    data.getAttachmentPaths().contains(attachment));
        });
    }

    @Test
    public void sendBacktraceReportWithDynamicAttributesRemovedDuringExecution() {
        BacktraceClient client = new BacktraceClient(context, credentials);
        String file1 = "file.txt";
        String file2 = "file2.txt";
        client.addAttachment(file1);
        client.addAttachment(file2);

        // Send first report with two attachments
        sendReportAndWait(client, new BacktraceReport(RESULT_MESSAGE), data -> {
            assertEquals(2, data.getAttachmentPaths().size());
            assertTrue(
                    "Expected attachment '" + file1 + "' not found",
                    data.getAttachmentPaths().contains(file1));
            assertTrue(
                    "Expected attachment '" + file2 + "' not found",
                    data.getAttachmentPaths().contains(file2));
        });

        // Remove attachment and send second report
        client.getAttachments().remove(file1);
        sendReportAndWait(client, new BacktraceReport(RESULT_MESSAGE), data -> {
            assertEquals(1, data.getAttachmentPaths().size());
            assertEquals(file2, data.getAttachmentPaths().get(0));
        });
    }
}
