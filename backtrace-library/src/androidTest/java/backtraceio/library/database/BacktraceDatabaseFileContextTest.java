package backtraceio.library.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;


@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseFileContextTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;
    private BacktraceDatabaseFileContext databaseFileContext;
    private final String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
        this.databaseContext = new BacktraceDatabaseContext(this.databaseSettings);
        this.databaseFileContext = new BacktraceDatabaseFileContext(this.dbPath, this.databaseSettings.getMaxDatabaseSize(), this.databaseSettings.getMaxRecordCount());
        this.databaseContext.clear();
        this.databaseFileContext.clear();
    }

    @After
    public void after() {
        this.databaseContext.clear();
    }


    @Test
    public void getFilesAfterAddOne() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        this.databaseContext.add(new BacktraceData(this.context, report, null));

        // WHEN
        int files = countAllFiles();

        // THEN
        assertEquals(3, files); // BacktraceDatabaseRecord, BacktraceData, BacktraceReport
    }

    @Test
    public void getFilesAfterAddThree() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);
        BacktraceReport report3 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));
        this.databaseContext.add(new BacktraceData(this.context, report3, null));

        // WHEN
        int files = countAllFiles();

        // THEN
        assertEquals(9, files); // 3x BacktraceDatabaseRecord, 3x BacktraceData, 3x BacktraceReport
    }

    @Test
    public void getRecords() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);
        BacktraceReport report3 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));
        this.databaseContext.add(new BacktraceData(this.context, report3, null));

        // WHEN
        int files = countRecords();

        // THEN
        assertEquals(3, files); // 3x BacktraceDatabaseRecord, 3x BacktraceData, 3x BacktraceReport
    }


    @Test
    public void clear() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));

        // WHEN
        int filesAfterAdd = countAllFiles();
        this.databaseFileContext.clear();
        int filesAfterClear = countAllFiles();


        // THEN
        assertEquals(6, filesAfterAdd); // 3x BacktraceDatabaseRecord, 3x BacktraceData, 3x BacktraceReport
        assertEquals(0, filesAfterClear); // 3x BacktraceDatabaseRecord, 3x BacktraceData, 3x BacktraceReport
    }

    @Test
    public void filesConsistency() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));

        // WHEN
        boolean result = this.databaseFileContext.validFileConsistency();

        // THEN
        assertTrue(result);
    }

    @Test
    public void forceInconsistencyMaxRecordCount() {
        // GIVEN
        this.databaseSettings.setMaxRecordCount(1);
        this.databaseFileContext = new BacktraceDatabaseFileContext(this.dbPath, this.databaseSettings.getMaxDatabaseSize(), this.databaseSettings.getMaxRecordCount());
        this.databaseContext = new BacktraceDatabaseContext(this.databaseSettings);

        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));

        // WHEN
        boolean result = this.databaseFileContext.validFileConsistency();

        // THEN
        assertFalse(result);
    }

    @Test
    public void forceInconsistencyMaxDatabaseSize() {
        // GIVEN
        this.databaseFileContext = new BacktraceDatabaseFileContext(this.dbPath, 1, this.databaseSettings.getMaxRecordCount());

        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);

        this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));

        // WHEN
        boolean result = this.databaseFileContext.validFileConsistency();

        // THEN
        assertFalse(result);
    }

    @Test
    public void removeOrphanedFiles() {
        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);

        final BacktraceDatabaseRecord record = this.databaseContext.add(new BacktraceData(this.context, report, null));
        this.databaseContext.add(new BacktraceData(this.context, report2, null));

        // WHEN
        int countRecords = countRecords();
        List<BacktraceDatabaseRecord> records = new ArrayList<BacktraceDatabaseRecord>() {{
            add(record);
        }};
        this.databaseFileContext.removeOrphaned(records);

        int countRecordAfterRemoveOrphaned = countRecords();
        int countFilesAfterRemoveOrphaned = countAllFiles();

        // THEN
        assertEquals(2, countRecords);
        assertEquals(1, countRecordAfterRemoveOrphaned);
        assertEquals(3, countFilesAfterRemoveOrphaned);
    }

    private int countRecords() {
        int files = 0;
        for (File ignored : this.databaseFileContext.getRecords()) {
            files++;
        }
        return files;
    }

    private int countAllFiles() {
        int files = 0;
        for (File ignored : this.databaseFileContext.getAll()) {
            files++;
        }
        return files;
    }
}