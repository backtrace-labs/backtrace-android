package backtraceio.library.database;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceDataAttachmentsFileHelper;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseRecordTest {
    private Context context;
    private BacktraceDatabase database;
    private final String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        String dbPath = this.context.getFilesDir().getAbsolutePath();
        BacktraceDatabaseSettings databaseSettings = new BacktraceDatabaseSettings(dbPath, RetryOrder.Queue);
        this.database = new BacktraceDatabase(this.context, databaseSettings);
    }

    @After
    public void after() {
        this.database.clear();
    }

    @Test
    public void saveAndGetRecord() {
        // GIVEN
        final BacktraceReport report = new BacktraceReport(testMessage);
        final BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        final BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());

        // WHEN
        final boolean saveResult = record.save();
        final boolean validResult = record.valid();
        record.close();

        final BacktraceData loadedData = record.getBacktraceData();

        // THEN
        assertTrue(saveResult);
        assertTrue(validResult);
        assertEquals(data.getReport().message, loadedData.getReport().message);
    }

    @Test
    public void saveAndGetRecordWithAttachments() {
        // GIVEN
        final String attachment0 = context.getFilesDir() + "/someFile.log";
        final String attachment1 = context.getFilesDir() + "/someOtherFile.log";
        final List<String> attachments = new ArrayList<String>() {
            {
                add(attachment0);
                add(attachment1);
            }
        };

        try {
            assertTrue(new File(attachment0).createNewFile());
            assertTrue(new File(attachment1).createNewFile());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        final BacktraceReport report = new BacktraceReport(testMessage, attachments);
        final BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        final BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());

        // WHEN
        final boolean saveResult = record.save();
        final boolean validResult = record.valid();
        record.close();

        final BacktraceData loadedData = record.getBacktraceData();
        final List<String> existingFiles = BacktraceDataAttachmentsFileHelper.getValidAttachments(context, loadedData);
        // THEN
        assertTrue(saveResult);
        assertTrue(validResult);
        assertEquals(data.getReport().message, loadedData.getReport().message);
        assertTrue(existingFiles.contains(attachment0));
        assertTrue(existingFiles.contains(attachment1));
    }

    @Test
    public void deleteFileDiagnosticPathToCorruptRecord() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());

        // WHEN
        boolean saveResult = record.save();
        boolean deleteResult = new File(record.getDiagnosticDataPath()).delete();
        boolean result = record.valid();

        // THEN
        assertTrue(saveResult);
        assertTrue(deleteResult);
        assertFalse(result);
    }

    @Test
    public void deleteFileReportPathToCorruptRecord() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());

        // WHEN
        boolean saveResult = record.save();
        boolean deleteResult = new File(record.getReportPath()).delete();
        boolean result = record.valid();

        // THEN
        assertTrue(saveResult);
        assertTrue(deleteResult);
        assertFalse(result);
    }

    @Test
    public void createAndDeleteRecordFiles() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());

        // WHEN
        boolean saveResult = record.save();
        int numberOfFilesAfterSave = new File(this.database.getSettings().getDatabasePath()).listFiles().length;

        record.delete();
        int numberOfFilesAfterDelete = new File(this.database.getSettings().getDatabasePath()).listFiles().length;

        // THEN
        assertTrue(saveResult);
        assertTrue(numberOfFilesAfterSave > 0);
        assertEquals(0, numberOfFilesAfterDelete);
    }

    @Test
    public void readFileAndDeserialize() {
        // GIVEN
        final BacktraceReport report = new BacktraceReport(testMessage);
        final BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        final BacktraceDatabaseRecord record =
                new BacktraceDatabaseRecord(data, this.database.getSettings().getDatabasePath());
        record.save();

        // WHEN
        final BacktraceDatabaseRecord recordFromFile =
                BacktraceDatabaseRecord.readFromFile(new File(record.getRecordPath()));
        final BacktraceData dataFromFile = recordFromFile.getBacktraceData();

        // THEN
        assertEquals(data.getReport().message, dataFromFile.getReport().message);
    }
}
