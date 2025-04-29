package backtraceio.library.database;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

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

    Context context = InstrumentationRegistry.getInstrumentation().getContext();
    @Before
    public void setUp() {
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings("test-path");
        settings.setRetryLimit(3);
        databaseContext = new BacktraceDatabaseContext(settings);
    }

    @Test
    public void testConcurrentModification() throws InterruptedException {
        // First populate the database with some records
        int recordCount = 1000;
        List<BacktraceDatabaseRecord> records = new ArrayList<>();

        for (int i = 0; i < recordCount; i++) {
            BacktraceData data = createMockBacktraceData();
            BacktraceDatabaseRecord record = databaseContext.add(data);
            records.add(record);
        }

        // Create a latch to synchronize threads
        CountDownLatch latch = new CountDownLatch(1);
        List<Exception> caughtExceptions = new ArrayList<>();

        List<Integer> deleted = new ArrayList<>();

        // Thread 1: Continuously deleting records
        int recordsToDelete = 750;
        Thread deleteThread = new Thread(() -> {
            try {
                latch.await(); // Wait for signal
                for (int i = 0; i < recordsToDelete; i++) {
                    databaseContext.delete(records.get(i));
                    deleted.add(1);
//                    Thread.sleep(20);
                }
//                for (BacktraceDatabaseRecord record : databaseContext.get()) {
//                    databaseContext.delete(record);
//                }
            } catch (Exception e) {
                synchronized (caughtExceptions) {
                    caughtExceptions.add(e);
                }
            }
        });

        int recordsToAdd = 500;
        List<Integer> added = new ArrayList<>();
        // Thread 2: Continuously adding new records
        Thread addThread = new Thread(() -> {
            try {
                latch.await(); // Wait for signal
                for (int i = 0; i < recordsToAdd; i++) {
                    BacktraceData data = createMockBacktraceData();
                    databaseContext.add(data);
                    added.add(1);
//                    Thread.sleep(5);
                }
            } catch (Exception e) {
                synchronized (caughtExceptions) {
                    caughtExceptions.add(e);
                }
            }
        });

        // Thread 3: Continuously getting records
        Thread getThread = new Thread(() -> {
            try {
                latch.await(); // Wait for signal
                String result;
                while(true) {
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

        // Start all threads
        deleteThread.start();
        addThread.start();
        getThread.start();

        // Release all threads simultaneously
        latch.countDown();

        // Wait for threads to complete
        deleteThread.join(5000);
        addThread.join(5000);
        getThread.join(5000);

        // Print all caught exceptions
        for (Exception e : caughtExceptions) {
            e.printStackTrace();
        }

        assertEquals(0, caughtExceptions.size());
        assertEquals(750, recordCount + added.size() - deleted.size());
        // Assert that we caught a ConcurrentModificationException
//        assertTrue("Expected ConcurrentModificationException",
//                caughtExceptions.stream()
//                        .anyMatch(e -> e instanceof ConcurrentModificationException ||
//                                (e.getCause() != null && e.getCause() instanceof ConcurrentModificationException)));
    }

    private BacktraceData createMockBacktraceData() {
        // Create a mock exception for the test
        Exception testException = new Exception("Test exception");

        // Create attributes map if needed
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("test_attribute", "test_value");

        // Create BacktraceData with the exception
        return new BacktraceData(
                context,
                new BacktraceReport(testException),
                attributes
        );
    }
}
