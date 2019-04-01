package backtraceio.library.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;


@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseRecordTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;
    private String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
        this.databaseContext = new BacktraceDatabaseContext(this.context, this.databaseSettings);
    }

    @Test
    public void saveAndGetRecord() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceData data  = new BacktraceData(this.context, report, null);
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
}
