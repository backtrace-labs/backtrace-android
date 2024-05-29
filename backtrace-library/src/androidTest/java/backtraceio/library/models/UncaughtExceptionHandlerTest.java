package backtraceio.library.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.models.types.BacktraceResultStatus;

public class UncaughtExceptionHandlerTest {

    private Context context;
    private BacktraceCredentials credentials;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    }

    private static void setRootHandler(Thread.UncaughtExceptionHandler customRootHandler, Thread.UncaughtExceptionHandler newRootHandler) {
        try {
            Field field = BacktraceExceptionHandler.class.getDeclaredField("rootHandler");
            field.setAccessible(true);
            field.set(customRootHandler, newRootHandler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private static BacktraceExceptionHandler createBacktraceExceptionHandler(BacktraceClient client) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Constructor<BacktraceExceptionHandler> constructor = BacktraceExceptionHandler.class.getDeclaredConstructor(BacktraceClient.class);
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        BacktraceExceptionHandler exceptionHandler = constructor.newInstance(client);
        setRootHandler(exceptionHandler, new SkipExceptionHandler());
        return exceptionHandler;
    }

    @Test()
    public void testUncaughtException() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        // GIVEN
        final Waiter waiter = new Waiter();
        final Exception exception = new IllegalArgumentException("Test message");
        final BacktraceClient client = new BacktraceClient(context, credentials);

        final AtomicReference<BacktraceData> testedAtomicReportData = new AtomicReference<>();
        client.setOnRequestHandler(data -> {
            testedAtomicReportData.set(data);
            waiter.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        });

        final BacktraceExceptionHandler handler = createBacktraceExceptionHandler(client);

        // WHEN
        handler.uncaughtException(Thread.currentThread(), exception);

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        // THEN
        assertNotNull(testedAtomicReportData);
        final BacktraceData testedReportData = testedAtomicReportData.get();
        assertEquals("Test message", testedReportdata.getReport().exception.getMessage());
        assertNull(testedReportdata.getReport().message);
        assertTrue(testedReportdata.getReport().diagnosticStack.size() > 0);
        assertEquals("java.lang.IllegalArgumentException", testedReportdata.getReport().classifier);
        assertEquals("Unhandled Exception", testedReportdata.getReport().attributes.get("error.type"));
        assertTrue(testedReportdata.getReport().exceptionTypeReport);
    }

    @Test
    public void testUncaughtError() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        // GIVEN
        final Waiter waiter = new Waiter();
        final Error error = new OutOfMemoryError();
        final BacktraceClient client = new BacktraceClient(context, credentials);

        final AtomicReference<BacktraceData> testedAtomicReportData = new AtomicReference<>();
        client.setOnRequestHandler(data -> {
            testedAtomicReportData.set(data);
            waiter.resume();
            return new BacktraceResult(data.getReport(), data.getReport().message,
                    BacktraceResultStatus.Ok);
        });

        final BacktraceExceptionHandler handler = createBacktraceExceptionHandler(client);

        // WHEN
        handler.uncaughtException(Thread.currentThread(), error);

        // WAIT FOR THE RESULT FROM ANOTHER THREAD
        try {
            waiter.await(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        // THEN
        assertNotNull(testedAtomicReportData);
        final BacktraceData testedReportData = testedAtomicReportData.get();
        assertNull(testedReportdata.getReport().message);
        assertTrue(testedReportdata.getReport().diagnosticStack.size() > 0);
        assertEquals("java.lang.OutOfMemoryError", testedReportdata.getReport().classifier);
        assertEquals("Unhandled Exception", testedReportdata.getReport().attributes.get("error.type"));
        assertTrue(testedReportdata.getReport().exceptionTypeReport);
    }
}
