package backtraceio.library.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseContext2Test {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;

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


    private void fillDatabase(int numberOfReports) {
        List<BacktraceDatabaseRecord> result = new ArrayList<>();

        for (int i = 0; i < numberOfReports; i++) {
            BacktraceReport report = new BacktraceReport(Integer.toString(i));
            BacktraceData data = new BacktraceData.Builder(report).setAttributes(this.context, null).build();
            result.add(databaseContext.add(data));
        }

        // Dispose all records
        for (BacktraceDatabaseRecord record : result) {
            record.close();
        }

    }
}
