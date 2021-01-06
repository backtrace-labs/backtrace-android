package backtraceio.library.database;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseProguardTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabase database;
    private String testMessage = "Example test string";

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
    public void addSingleRecordNoProguard() {
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());

        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);

        // WHEN
        database.add(report, null);

        // THEN
        assertEquals(report, database.get().iterator().next().getBacktraceData().report);
        assertNull(database.get().iterator().next().getBacktraceData().symbolication);
        assertEquals(testMessage, database.get().iterator().next().getBacktraceData().report.message);
        assertEquals(1, database.count());
    }

    @Test
    public void addSingleRecordProguard() {
        assertEquals(0, database.getDatabaseSize());
        assertEquals(0, database.count());

        // GIVEN
        BacktraceReport report = new BacktraceReport(testMessage);

        // WHEN
        database.add(report, null, true);

        // THEN
        assertEquals(report, database.get().iterator().next().getBacktraceData().report);
        assertEquals("proguard", database.get().iterator().next().getBacktraceData().symbolication);
        assertEquals(testMessage, database.get().iterator().next().getBacktraceData().report.message);
        assertEquals(1, database.count());
    }
}