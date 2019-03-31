package backtraceio.library.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseContextTest {
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
    }

    @Test
    public void LastFromDatabaseContextQueue() {
        // GIVEN
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord last = databaseContext.last(); // QUEUE

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(3), last);
    }

    @Test
    public void firstFromDatabaseContextStack() {
        // GIVEN
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Stack);
        this.databaseContext = new BacktraceDatabaseContext(this.context, settings);
        List<BacktraceDatabaseRecord> records = fillDatabase();

        // WHEN
        BacktraceDatabaseRecord firstOnStack = databaseContext.first(); // STACK

        // THEN
        assertEquals(3, databaseContext.count());
        assertEquals(records.get(3), firstOnStack);
    }

    @Test
    public void lastFromDatabaseContextStack() {
        // GIVEN
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Stack);
        this.databaseContext = new BacktraceDatabaseContext(this.context, settings);
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
        Iterable<BacktraceDatabaseRecord> contextRecords = databaseContext.get(); // STACK

        // THEN
        assertEquals(3, databaseContext.count());
        for(BacktraceDatabaseRecord record : contextRecords)
        {
            assertTrue(records.contains(record));
        }
    }

    private List<BacktraceDatabaseRecord> fillDatabase()
    {
        List<BacktraceDatabaseRecord> result = new ArrayList<>();
        BacktraceReport report = new BacktraceReport(this.testMessage);
        BacktraceReport report2 = new BacktraceReport(this.testMessage);
        BacktraceReport report3 = new BacktraceReport(this.testMessage);
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceData data2 = new BacktraceData(this.context, report2, null);
        BacktraceData data3 = new BacktraceData(this.context, report3, null);
        result.add(databaseContext.add(data));
        result.add(databaseContext.add(data2));
        result.add(databaseContext.add(data3));
        return result;
    }
}