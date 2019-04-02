package backtraceio.library.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;


@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseFileContextTest {
    private Context context;
    private String dbPath;
    private BacktraceDatabaseContext databaseContext;
    private BacktraceDatabaseSettings databaseSettings;
    private BacktraceDatabaseFileContext databaseFileContext;
    private String testMessage = "Example test string";

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.dbPath = this.context.getFilesDir().getAbsolutePath();
        this.databaseSettings = new BacktraceDatabaseSettings(this.dbPath, RetryOrder.Queue);
        this.databaseContext = new BacktraceDatabaseContext(this.context, this.databaseSettings);
        this.databaseFileContext = new BacktraceDatabaseFileContext(this.dbPath, this.databaseSettings.getMaxDatabaseSize(), this.databaseSettings.maxRecordCount);
    }

    @After
    public void after() {
        this.databaseContext.clear();
    }


    @Test
    public void getFiles(){

    }
}