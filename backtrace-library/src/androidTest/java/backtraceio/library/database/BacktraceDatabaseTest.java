package backtraceio.library.database;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabase database;
    private final String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.database = new BacktraceDatabase(this.context, dbPath);
        this.database.start();
        this.database.clear();
    }

    @After
    public void after() {
        this.database.clear();
    }


    @Test
    public void isDatabaseEmpty() {
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());
    }

    @Test
    public void addSingleRecord() {
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());

        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);

        // WHEN
        database.add(report, null);

        // THEN
        assertEquals(report, database.get().iterator().next().getBacktraceData(context).report);
        assertEquals(testMessage, database.get().iterator().next().getBacktraceData(context).report.message);
        assertEquals(1, database.count());
    }

    @Test
    public void addWithAttributes() {
        // GIVEN
        String key = "Example key";
        String value = "Example value";
        BacktraceReport report = new BacktraceReport(testMessage);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(key, value);

        // WHEN
        BacktraceDatabaseRecord record = database.add(report, attributes);
        BacktraceData dataFromDatabase = record.getBacktraceData(context);

        // THEN
        assertEquals(value, dataFromDatabase.attributes.get(key));
    }

    @Test
    public void deleteSingleRecord() {
        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(new Exception("Example exception"));

        BacktraceDatabaseRecord record = database.add(report, null);
        BacktraceDatabaseRecord record2 = database.add(report2, null);
        assertEquals(2, database.count());

        database.delete(record);
        assertEquals(1, database.count());

        BacktraceDatabaseRecord recordFromDatabase = database.get().iterator().next();
        assertEquals(record2, recordFromDatabase);
        assertEquals(report2, recordFromDatabase.getBacktraceData(context).report);
        assertEquals(report2.exception.getMessage(), recordFromDatabase.getBacktraceData(context).report.exception.getMessage());
    }


    @Test
    public void clearDatabase() {
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());

        BacktraceReport report = new BacktraceReport(testMessage);

        database.add(report, null);
        database.add(report, null);
        assertEquals(2, database.count());

        database.clear();
        int filesNumber = new File(this.dbPath).listFiles().length;
        assertEquals(0, filesNumber);
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());
    }

    @Test
    public void flushDatabase() {

        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, this.database);

        BacktraceReport report = new BacktraceReport(testMessage);
        BacktraceReport report2 = new BacktraceReport(testMessage);
        BacktraceReport report3 = new BacktraceReport(testMessage);

        BacktraceDatabaseRecord record = database.add(report, null);
        BacktraceDatabaseRecord record2 = database.add(report2, null);
        BacktraceDatabaseRecord record3 = database.add(report3, null);

        final List<Integer> requestsCounter = new ArrayList<>();
        backtraceClient.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                requestsCounter.add(1);
                waiter.resume();
                return null;
            }
        });

        record.close();
        record2.close();
        record3.close();

        // WHEN
        database.flush();

        // THEN
        try {
            waiter.await(2000, 3);
        } catch (Exception e) {
            e.printStackTrace();
            waiter.fail(e);
        }
        assertEquals(3, requestsCounter.size());
        assertEquals(0, database.count());
    }

    @Test
    public void recordLimit() {
        // GIVEN
        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
        settings.setMaxRecordCount(1);
        this.database = new BacktraceDatabase(this.context, settings);
        this.database.start();
        this.database.clear();

        BacktraceReport report = new BacktraceReport("first");
        BacktraceReport report2 = new BacktraceReport("second");

        // WHEN
        BacktraceDatabaseRecord record = database.add(report, null);
        record.close();

        BacktraceDatabaseRecord record2 = database.add(report2, null);
        record2.close();

        // THEN
        assertEquals(1, database.count());
        assertEquals(report2.message, database.get().iterator().next().getBacktraceData(context).report.message);
    }

}