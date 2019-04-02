package backtraceio.library.database;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import backtraceio.library.common.FileHelper;
import backtraceio.library.models.database.BacktraceDatabaseRecordWriter;

import backtraceio.library.services.BacktraceDatabaseFileContext;

import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseRecordWriterTest {

    private String dbPath;
    private BacktraceDatabaseFileContext databaseFileContext;
    private BacktraceDatabaseRecordWriter databaseRecordWriter;

    @Before
    public void setUp() {
        this.dbPath = InstrumentationRegistry.getContext().getFilesDir().getAbsolutePath();
        this.databaseFileContext = new BacktraceDatabaseFileContext(this.dbPath, 0, 0);
        this.databaseFileContext.clear();
        this.databaseRecordWriter = new BacktraceDatabaseRecordWriter(this.dbPath);
    }

    @After
    public void after() {
        this.databaseFileContext.clear();
    }

    @Test
    public void writeBytes() throws IOException {
        // GIVEN
        String fileContent = "Example string";
        int filesBeforeWrite = countFiles().size();

        // WHEN
        String filePath = this.databaseRecordWriter.write(fileContent.getBytes(), "test_file");
        int filesAfterWrite = countFiles().size();
        String contentFromFile = FileHelper.readFile(new File(filePath));

        // THEN
        assertEquals(0, filesBeforeWrite);
        assertEquals(1, filesAfterWrite);
        assertEquals(fileContent, contentFromFile);
    }

    @Test
    public void writeObject() throws IOException {
        // GIVEN
        Exception exception = new Exception("Example message");
        String expectedResult = "{\"detail-message\":\"Example message\",\"stack-trace\":[],\"suppressed-exceptions\":[]}";

        // WHEN
        String filePath = this.databaseRecordWriter.write(exception, null);
        String jsonResult = FileHelper.readFile(new File(filePath));

        // THEN
        assertEquals(expectedResult, jsonResult);
    }

    private List<File> countFiles() {
        return Arrays.asList(new File(this.dbPath).listFiles());
    }
}
