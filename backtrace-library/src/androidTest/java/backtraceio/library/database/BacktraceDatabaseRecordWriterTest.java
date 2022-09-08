package backtraceio.library.database;

import static junit.framework.TestCase.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.FileHelper;
import backtraceio.library.models.database.BacktraceDatabaseRecordWriter;
import backtraceio.library.services.BacktraceDatabaseFileContext;

@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseRecordWriterTest {

    private String dbPath;
    private BacktraceDatabaseFileContext databaseFileContext;
    private BacktraceDatabaseRecordWriter databaseRecordWriter;

    @Before
    public void setUp() {
        this.dbPath = InstrumentationRegistry.getInstrumentation().getContext().getFilesDir().getAbsolutePath();
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
    public void writeObject() throws Exception {
        // GIVEN
        Exception exception = new Exception("Example message");
        StackTraceElement element = new StackTraceElement("Exception.class", "writeObject", "BacktraceDatabaseRecordWriterTest.java", 1);
        StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = element;

        exception.setStackTrace(stackTraceElements);
        exception.addSuppressed(new IllegalArgumentException("illegal-argument"));

        // WHEN
        String filePath = this.databaseRecordWriter.write(exception, null);
        String jsonResult = FileHelper.readFile(new File(filePath));
        Exception exceptionResult = BacktraceSerializeHelper.fromJson(jsonResult, Exception.class);

        // THEN
        assertEquals(exception.getMessage(), exceptionResult.getMessage());

        assertEquals(exception.getStackTrace().length, exceptionResult.getStackTrace().length);
        assertEquals(exception.getStackTrace()[0].getLineNumber(), exceptionResult.getStackTrace()[0].getLineNumber());
        assertEquals(exception.getStackTrace()[0].getFileName(), exceptionResult.getStackTrace()[0].getFileName());
        assertEquals(exception.getStackTrace()[0].getMethodName(), exceptionResult.getStackTrace()[0].getMethodName());
        assertEquals(exception.getStackTrace()[0].getClassName(), exceptionResult.getStackTrace()[0].getClassName());

        assertEquals(exception.getSuppressed().length, exceptionResult.getSuppressed().length);
        assertEquals(exception.getSuppressed()[0].getMessage(), exceptionResult.getSuppressed()[0].getMessage());
    }

    private List<File> countFiles() {
        return Arrays.asList(new File(this.dbPath).listFiles());
    }
}
