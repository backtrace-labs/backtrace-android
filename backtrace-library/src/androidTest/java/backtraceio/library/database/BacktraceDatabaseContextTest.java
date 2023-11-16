package backtraceio.library.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseContextTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;
    private final String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
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
    public void multithreadedTest() throws InterruptedException {
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Stack);
        this.databaseContext = new BacktraceDatabaseContext(settings);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    BacktraceData data = new BacktraceData(context, new BacktraceReport(Integer.toString(i)), null);
                    databaseContext.add(data);
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch1.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    BacktraceDatabaseRecord record = databaseContext.last();
                    if (record != null) {
                        databaseContext.add(record);
                    }
                    BacktraceDatabaseRecord record3 = databaseContext.first();
                    BacktraceDatabaseRecord record2 = databaseContext.last();
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch2.countDown();
            }
        });


        Thread t3 = new Thread(() -> {
            try {
                Thread.sleep(2000);
                databaseContext.get().forEach(x-> {
                    x.locked = false;

                });

                for (int i = 0; i < 100; i++) {
                    BacktraceDatabaseRecord record = databaseContext.first();
                    if(record == null) {
                        continue;
                    }
                    record.locked = false;
                    databaseContext.delete(record);
                    databaseContext.delete(record);
                    databaseContext.delete(record);
                    BacktraceData data = new BacktraceData(context, new BacktraceReport(Integer.toString(i)), null);
//                    databaseContext.add(data);
//                    Thread.sleep(200);

                    BacktraceDatabaseRecord record2 = databaseContext.last();
                    BacktraceDatabaseRecord record3 = databaseContext.first();
                    if(record2 == null) {
                        continue;
                    }

                    if(record3 == null) {
                        continue;
                    }
                    databaseContext.delete(record2);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch3.countDown();
            }
        });


        t1.start();
        t2.start();
        t3.start();

        latch1.await();
        latch2.await();
        latch3.await();
        System.out.println("END");
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
        BacktraceData data = new BacktraceData(this.context, report, null);
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
        BacktraceData data = new BacktraceData(this.context, report, null);
        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(data, this.dbPath);

        // WHEN
        databaseContext.delete(record);

        // THEN
        assertEquals(3, databaseContext.count());
    }

    @Test
    public void xyz() throws Exception{ // todo: fix name
        fillDatabase();

//        final List<BacktraceDatabaseRecord> records = (List) databaseContext.get();
//        BacktraceDatabaseRecord x = databaseContext.first();
        databaseContext.clear();
//        x = null;
//        System.gc();
//        ((List)databaseContext.get()).set(0, null);
//        databaseContext.removeOldestRecord();

//
//        databaseContext.removeOldestRecord();

//
//        databaseContext.removeOldestRecord();
////        databaseContext.

//        final List<BacktraceDatabaseRecord> records2 = (List) databaseContext.get();

        for (BacktraceDatabaseRecord rec : databaseContext.get()) {
            if (rec == null) {
                throw new Exception("NULL");
            }
        }

    }

    @Test(expected = NullPointerException.class)
    public void tryAddNullBacktraceData() {
        databaseContext.add((BacktraceData) null);
    }

    @Test(expected = NullPointerException.class)
    public void tryAddNullBacktraceDbRecord() {
        databaseContext.add((BacktraceDatabaseRecord) null);
    }

    private List<BacktraceDatabaseRecord> fillDatabase() {
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



        // Dispose all records
        for (BacktraceDatabaseRecord record : result) {
            record.close();
        }

        return result;
    }
}