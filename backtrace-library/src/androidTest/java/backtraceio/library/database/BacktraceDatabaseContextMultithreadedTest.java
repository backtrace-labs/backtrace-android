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
    private static class TestConfig {
        final int recordsState;
        final int recordsToAdd;
        final int recordsToDelete;
        final int threadWaitTimeMs;

        TestConfig(int recordsState, int recordsToAdd, int recordsToDelete, int threadWaitTimeMs) {
            this.recordsState = recordsState;
            this.recordsToAdd = recordsToAdd;
            this.recordsToDelete = recordsToDelete;
            this.threadWaitTimeMs = threadWaitTimeMs;
        }
    }

    private static class ConcurrentTestState {
        final List<Exception> caughtExceptions = new ArrayList<>();
        final List<Integer> deletedRecords = new ArrayList<>();
        final List<Integer> addedRecords = new ArrayList<>();

        synchronized void handleException(Exception e) {
            caughtExceptions.add(e);
        }

        void printExceptions() {
            for (Exception e : caughtExceptions) {
                e.printStackTrace();
            }
        }
    }
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
        final TestConfig config = new TestConfig(500, 250, 150, 30000); // 30s
        final List<BacktraceDatabaseRecord> initialRecords = generateMockRecords(config.recordsState);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final ConcurrentTestState testState = new ConcurrentTestState();

        // Create and start test threads
        Thread addThread = createAddThread(startLatch, config.recordsToAdd, testState);
        Thread deleteThread = createDeleteThread(startLatch, initialRecords, config.recordsToDelete, testState);
        Thread readThread = createReadThread(startLatch, testState);

        // WHEN
        startThreads(deleteThread, addThread, readThread);
        startLatch.countDown();
        waitForThreads(deleteThread, addThread, readThread, config.threadWaitTimeMs);

        // Print any exceptions that occurred
        testState.printExceptions();

        // THEN
        assertTestResults(config, testState);
    }

    private Thread createDeleteThread(CountDownLatch latch, List<BacktraceDatabaseRecord> records,
            int recordsToDelete, ConcurrentTestState state) {
        return new Thread(() -> {
            try {
                latch.await();
                for (int i = 0; i < recordsToDelete; i++) {
                    databaseContext.delete(records.get(i));
                    state.deletedRecords.add(1);
                }
            } catch (Exception e) {
                state.handleException(e);
            }
        });
    }

    private Thread createAddThread(CountDownLatch latch, int recordsToAdd, ConcurrentTestState state) {
        return new Thread(() -> {
            try {
                latch.await();
                for (int i = 0; i < recordsToAdd; i++) {
                    BacktraceData data = createMockBacktraceData();
                    databaseContext.add(data);
                    state.addedRecords.add(1);
                }
            } catch (Exception e) {
                state.handleException(e);
            }
        });
    }

    private Thread createReadThread(CountDownLatch latch, ConcurrentTestState state) {
        return new Thread(() -> {
            try {
                latch.await();
                String result;
                while (true) {
                    for (BacktraceDatabaseRecord record : databaseContext.get()) {
                        result = record.toString();
                    }
                }
            } catch (Exception e) {
                state.handleException(e);
            }
        });
    }

    private void startThreads(Thread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    private void waitForThreads(Thread deleteThread, Thread addThread, Thread readThread, int waitTimeMs)
            throws InterruptedException {
        deleteThread.join(waitTimeMs);
        addThread.join(waitTimeMs);
        readThread.join(waitTimeMs);
    }

    private void assertTestResults(TestConfig config, ConcurrentTestState state) {
        assertEquals(0, state.caughtExceptions.size());
        assertEquals(
            config.recordsState + config.recordsToAdd - config.recordsToDelete,
            config.recordsState + state.addedRecords.size() - state.deletedRecords.size()
        );
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
