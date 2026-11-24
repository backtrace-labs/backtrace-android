package backtraceio.library.database;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.BacktraceMockLogger;
import backtraceio.library.logger.Logger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BacktraceDatabaseFailedSaveTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Logger previousLogger;
    private File databaseDirectory;
    private File attachment;
    private BacktraceData data;
    private BacktraceDatabaseContext database;

    @Before
    public void setUp() throws IOException {
        previousLogger = BacktraceLogger.getLogger();
        BacktraceLogger.setLogger(new BacktraceMockLogger());
        databaseDirectory = temporary.newFolder("database");
        attachment = temporary.newFile("caller-owned.txt");
        Files.write(attachment.toPath(), new byte[] {12, 34});

        BacktraceReport report =
                new BacktraceReport("capture-origin", Collections.singletonList(attachment.getAbsolutePath()));
        data = new BacktraceData(report.uuid.toString(), null, 1234, "", "fixture",
                Collections.singletonMap("scope", "origin"), "original-thread", new String[0], report,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        database = new BacktraceDatabaseContext(new BacktraceDatabaseSettings(databaseDirectory.getAbsolutePath()));
    }

    @After
    public void restoreLogger() {
        BacktraceLogger.setLogger(previousLogger);
    }

    @Test
    public void diagnosticRenameFailureDoesNotCreateRetryRecord() throws IOException {
        assertFailedSaveLeavesOnlyForeignBlocker("attachment");
    }

    @Test
    public void reportRenameFailureRemovesPartialDiagnosticData() throws IOException {
        assertFailedSaveLeavesOnlyForeignBlocker("report");
    }

    @Test
    public void metadataRenameFailureRemovesBothPartialPayloads() throws IOException {
        assertFailedSaveLeavesOnlyForeignBlocker("record");
    }

    @Test
    public void missingReportDoesNotCreateRetryRecord() {
        data.report = null;

        assertNull(database.add(data));
        assertFalse(database.get().iterator().hasNext());
        assertEquals(0, database.getDatabaseSize());
        assertArrayEquals(new String[0], databaseDirectory.list());
    }

    @Test
    public void successfulSaveReopensOriginalSnapshot() throws IOException {
        BacktraceDatabaseRecord written = database.add(data);
        assertNotNull(written);
        assertEquals(3, databaseDirectory.list().length);

        BacktraceDatabaseRecord reopened = BacktraceDatabaseRecord.readFromFile(new File(written.getRecordPath()));
        assertNotNull(reopened);
        BacktraceData loaded = reopened.getBacktraceData();
        assertNotNull(loaded);
        assertEquals(data.uuid, loaded.uuid);
        assertEquals(data.timestamp, loaded.timestamp);
        assertEquals("origin", loaded.attributes.get("scope"));
        assertEquals("original-thread", loaded.mainThread);
        assertEquals("capture-origin", loaded.getReport().message);
        assertEquals(Collections.singletonList(attachment.getAbsolutePath()), loaded.getAttachmentPaths());
    }

    private void assertFailedSaveLeavesOnlyForeignBlocker(String suffix) throws IOException {
        String filename = data.uuid + "-" + suffix + ".json";
        File blocker = temporary.newFolder(databaseDirectory.getName(), filename);
        File unrelatedContent = new File(blocker, "sentinel.txt");
        Files.write(unrelatedContent.toPath(), "keep".getBytes(StandardCharsets.UTF_8));

        assertNull(database.add(data));
        assertFalse(database.get().iterator().hasNext());
        assertEquals(0, database.getDatabaseSize());
        assertArrayEquals(new String[] {filename}, databaseDirectory.list());
        assertEquals("keep", new String(Files.readAllBytes(unrelatedContent.toPath()), StandardCharsets.UTF_8));
        assertArrayEquals(new byte[] {12, 34}, Files.readAllBytes(attachment.toPath()));
    }
}
