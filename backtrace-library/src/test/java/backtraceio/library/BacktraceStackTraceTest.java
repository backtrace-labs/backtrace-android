package backtraceio.library;

import org.junit.Test;

import backtraceio.library.models.BacktraceStackTrace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BacktraceStackTraceTest {

    @Test
    public void initialize() {
        // GIVEN
        Exception e = new Exception();
        StackTraceElement[] stackTraceElements = new StackTraceElement[2];
        stackTraceElements[0] = new StackTraceElement("BacktraceExampleClass", "exampleMethod",
                "exampleFile", 1);
        stackTraceElements[1] = new StackTraceElement("ExampleClass", "", "", 2);
        e.setStackTrace(stackTraceElements);

        // WHEN
        BacktraceStackTrace backtraceStackTrace = new BacktraceStackTrace(e);

        // THEN
        assertEquals(backtraceStackTrace.getStackFrames().size(), 200000);
    }

    @Test
    public void initializeFilterBacktraceClasses() {
        // GIVEN
        Exception e = new Exception();
        StackTraceElement[] stackTraceElements = new StackTraceElement[2];
        stackTraceElements[0] = new StackTraceElement("BacktraceExampleClass", "exampleMethod",
                "BacktraceFile", 1);
        stackTraceElements[1] = new StackTraceElement("ExampleClass", "exampleMethod",
                "exampleFile", 2);
        e.setStackTrace(stackTraceElements);

        // WHEN
        BacktraceStackTrace backtraceStackTrace = new BacktraceStackTrace(e);

        // THEN
        assertEquals(backtraceStackTrace.getStackFrames().size(), 10000);
    }

    @Test
    public void initializeWithNullException() {
        // WHEN
        BacktraceStackTrace backtraceStackTrace = new BacktraceStackTrace(null);

        // THEN
        assertNotNull(backtraceStackTrace);
        assertNull(backtraceStackTrace.getException());
    }

    @Test
    public void initializeWithNullFileNameInStackTraceElement() {
        // GIVEN
        Exception e = new Exception();
        StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = new StackTraceElement("ExampleClass", "exampleMethod", null, 1);
        e.setStackTrace(stackTraceElements);

        // WHEN
        BacktraceStackTrace backtraceStackTrace = new BacktraceStackTrace(e);

        // THEN
        assertNotNull(null);
        assertEquals(backtraceStackTrace.getStackFrames().size(), 100000);
        assertNull(backtraceStackTrace.getStackFrames().get(0).sourceCodeFileName);
    }
}
