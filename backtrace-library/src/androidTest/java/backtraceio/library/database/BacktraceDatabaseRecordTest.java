package backtraceio.library.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;


@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseRecordTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseSettings databaseSettings;
    private BacktraceDatabase database;
    private String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
        this.database = new BacktraceDatabase(this.context, dbPath);
    }

    @After
    public void after() {
        this.database.clear();
    }

    @Test
    public void saveAndGetRecord() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

        // WHEN
        boolean saveResult = record.save();
        boolean validResult = record.valid();
        record.close();

        BacktraceData loadedData = record.getBacktraceData();

        // THEN
        assertTrue(saveResult);
        assertTrue(validResult);
        assertEquals(data.report.message, loadedData.report.message);
    }

    @Test
    public void deleteFileDiagnosticPathToCorruptRecord() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

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
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

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
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

        // WHEN
        boolean saveResult = record.save();
        int numberOfFilesAfterSave = new File(this.dbPath).listFiles().length;

        record.delete();
        int numberOfFilesAfterDelete = new File(this.dbPath).listFiles().length;

        // THEN
        assertTrue(saveResult);
        assertTrue(numberOfFilesAfterSave > 0);
        assertEquals(0, numberOfFilesAfterDelete);
    }

    @Test
    public void readFileAndDeserialize() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);
        record.save();

        // WHEN
        BacktraceDatabaseRecord recordFromFile = BacktraceDatabaseRecord.readFromFile(new File(record.getRecordPath()));
        BacktraceData dataFromFile = recordFromFile.getBacktraceData();

        // THEN
        assertEquals(data.report.message, dataFromFile.report.message);
    }
}
