package backtraceio.library.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseContextTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;
    private final String testMessage = "Example test string";

    private final int RETRY_LIMIT = 5;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
        this.databaseSettings.setRetryLimit(RETRY_LIMIT);
        this.databaseContext = new BacktraceDatabaseContext(this.databaseSettings);
    }

    @After
    public void after() {
        this.databaseContext.clear();
    }

    @Test
    public void firstFromDatabaseContextQueue() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord first = databaseContext.first(); // QUEUE

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(0), first);
        assertNotEquals(records.get(1), first);
        assertNotEquals(records.get(2), first);
    }

    @Test
    public void lastFromDatabaseContextQueue() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord last = databaseContext.last(); // QUEUE

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(2), last);
    }

    @Test
    public void firstFromDatabaseContextStack() {
        // GIVEN
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Stack);
        this.databaseContext = new BacktraceDatabaseContext(settings);
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord firstOnStack = databaseContext.first(); // STACK

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(2), firstOnStack);
    }

    @Test
    public void lastFromDatabaseContextStack() {
        // GIVEN
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Stack);
        this.databaseContext = new BacktraceDatabaseContext(settings);
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord lastOnStack = databaseContext.last(); // STACK

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(0), lastOnStack);
    }

    @Test
    public void getFromDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        Iterable<BacktraceDatabaseRecord> contextRecords = databaseContext.get();

        // THEN
        assertEquals(3, databaseContext.count());
        for (BacktraceDatabaseRecord record : contextRecords) {
            assertTrue(records.contains(record));
        }
    }

    @Test
    public void containsInDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        boolean result = databaseContext.contains(records.get(1));

        // THEN
        assertTrue(result);
    }

    @Test(expected = NullPointerException.class)
    public void containsNullInDatabaseContext() {
        // GIVEN
        fillDatabase();

        // WHEN
        databaseContext.contains(null);
    }

    @Test
    public void notContainsInDatabaseContext() {
        // GIVEN
        fillDatabase();
        BacktraceReport report = new BacktraceReport(this.testMessage);
        BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

        // WHEN
        boolean result = databaseContext.contains(record);

        // THEN
        assertFalse(result);
    }

    @Test
    public void isEmptyDatabaseContext() {
        assertTrue(this.databaseContext.isEmpty());

        // GIVEN
        fillDatabase();

        // WHEN
        boolean result = databaseContext.isEmpty();

        // THEN
        assertFalse(result);
    }

    @Test
    public void removeOldestFromDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        boolean result = databaseContext.removeOldestRecord();

        // THEN
        assertEquals(2, databaseContext.count());
        assertTrue(result);
        assertTrue(databaseContext.contains(records.get(1)));
        assertTrue(databaseContext.contains(records.get(2)));
    }

    @Test
    public void deleteFromDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        databaseContext.delete(records.get(0));

        // THEN
        assertEquals(2, databaseContext.count());
        assertTrue(databaseContext.contains(records.get(1)));
        assertTrue(databaseContext.contains(records.get(2)));
    }

    @Test
    public void delete2RecordsFromDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        boolean result1 = databaseContext.delete(records.get(0));
        boolean result2 = databaseContext.delete(records.get(1));

        // THEN
        assertTrue(result1);
        assertTrue(result2);
        assertEquals(1, databaseContext.count());
        assertTrue(databaseContext.contains(records.get(2)));
    }

    @Test
    public void deleteSameRecordFromDatabaseContext() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        databaseContext.delete(records.get(0));
        databaseContext.delete(records.get(0));

        // THEN
        assertEquals(2, databaseContext.count());
        assertTrue(databaseContext.contains(records.get(1)));
        assertTrue(databaseContext.contains(records.get(2)));
    }

    @Test
    public void tryDeleteNotExistingRecordFromDatabaseContext() {
        // GIVEN
        fillDatabase();
        BacktraceReport report = new BacktraceReport(this.testMessage);
        BacktraceData data = new BacktraceData.Builder(report)
                .setAttributes(this.context, null)
                .build();
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

        // WHEN
        databaseContext.delete(record);

        // THEN
        assertEquals(3, databaseContext.count());
    }

    @Test(expected = NullPointerException.class)
    public void tryAddNullBacktraceData() {
        databaseContext.add((BacktraceData) null);
    }

    @Test(expected = NullPointerException.class)
    public void tryAddNullBacktraceDbRecord() {
        databaseContext.add((BacktraceDatabaseRecord) null);
    }

    @Test
    public void testIncrementBatchRetry() {
        // GIVEN
        int numberOfRetries = 0;
        fillDatabase(10);

        // WHEN

        for (int i = 0; i < 10; i++) {
            BacktraceDatabaseRecord first = databaseContext.first(); // QUEUE
            if (first == null) {
                break;
            }
            first.close();
            this.databaseContext.incrementBatchRetry();
            numberOfRetries++;
        }

        // THEN
        assertEquals(0, databaseContext.count());
        assertEquals(RETRY_LIMIT, numberOfRetries);
    }

    private List<BacktraceDatabaseRecord> fillDatabase() {
        return this.fillDatabase(3);
    }

    private List<BacktraceDatabaseRecord> fillDatabase(int numberOfReports) {
        List<BacktraceDatabaseRecord> result = new ArrayList<>();

        for (int i = 0; i < numberOfReports; i++) {
            BacktraceReport report = new BacktraceReport(testMessage);
            BacktraceData data = new BacktraceData.Builder(report)
                    .setAttributes(this.context, null)
                    .build();
            result.add(databaseContext.add(data));
        }

        // Dispose all records
        for (BacktraceDatabaseRecord record : result) {
            record.close();
        }

        return result;
    }
}
