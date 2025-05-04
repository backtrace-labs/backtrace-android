package backtraceio.library.database;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;

public class BacktraceDatabaseContextMultithreadedTest {
    private BacktraceDatabaseContext databaseContext;

    @Before
    public void setUp() {
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings("test-path");
        settings.setRetryLimit(3);
        databaseContext = new BacktraceDatabaseContext(settings);
    }

    @Test
    public void testConcurrentModification() throws InterruptedException {
        // GIVEN
        final int recordsState = 1000;
        final int recordsToAdd = 500;
        final int recordsToDelete = 750;
        final int threadWaitTimeMs = 5000;
        final List<BacktraceDatabaseRecord> records = generateMockRecords(recordsState);

        final CountDownLatch latch = new CountDownLatch(1);

        final List<Exception> caughtExceptions = new ArrayList<>();
        final List<Integer> deletedRecords = new ArrayList<>();
        final List<Integer> addedRecords = new ArrayList<>();

        // GIVEN threads
        final Thread deleteThread = new Thread(() -> {
            try {
                latch.await();
                for (int i = 0; i < recordsToDelete; i++) {
                    databaseContext.delete(records.get(i));
                    deletedRecords.add(1);
                }
            } catch (Exception e) {
                synchronized (caughtExceptions) {
                    caughtExceptions.add(e);
                }
            }
        });

        final Thread addThread = new Thread(() -> {
            try {
                latch.await();
                for (int i = 0; i < recordsToAdd; i++) {
                    BacktraceData data = createMockBacktraceData();
                    databaseContext.add(data);
                    addedRecords.add(1);
                }
            } catch (Exception e) {
                synchronized (caughtExceptions) {
                    caughtExceptions.add(e);
                }
            }
        });

        final Thread readThread = new Thread(() -> {
            try {
                latch.await();
                String result;
                while (true) {
                    for (BacktraceDatabaseRecord record : databaseContext.get()) {
                        result = record.toString();
                    }
                }
            } catch (Exception e) {
                synchronized (caughtExceptions) {
                    caughtExceptions.add(e);
                }
            }
        });

        // WHEN
        // Start all threads
        deleteThread.start();
        addThread.start();
        readThread.start();

        // Release all threads simultaneously
        latch.countDown();

        // Wait for threads to complete
        deleteThread.join(threadWaitTimeMs);
        addThread.join(threadWaitTimeMs);
        readThread.join(threadWaitTimeMs);

        // Print all caught exceptions
        for (Exception e : caughtExceptions) {
            e.printStackTrace();
        }

        // THEN
        assertEquals(0, caughtExceptions.size());
        assertEquals(recordsState + recordsToAdd - recordsToDelete, recordsState + addedRecords.size() - deletedRecords.size());
    }

    @NonNull
    private List<BacktraceDatabaseRecord> generateMockRecords(int recordCount) {
        final List<BacktraceDatabaseRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            BacktraceData data = createMockBacktraceData();
            BacktraceDatabaseRecord record = databaseContext.add(data);
            records.add(record);
        }
        return records;
    }

    private BacktraceData createMockBacktraceData() {
        final Exception testException = new Exception("Test exception");

        final Map<String, Object> attributes = new HashMap<String, Object>() {{
            put("test_attribute", "test_value");
        }};

        return new BacktraceData.Builder(new BacktraceReport(testException, attributes)).build();
    }
}
