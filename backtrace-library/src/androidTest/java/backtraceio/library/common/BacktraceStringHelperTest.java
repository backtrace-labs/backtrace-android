package backtraceio.library.common;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceStringHelperTest {

    @Test
    public void testStringIsNull() {
        String nullString = null;
        assertTrue(BacktraceStringHelper.isNullOrEmpty(nullString));
    }

    @Test
    public void testStringIsEmpty() {
        String emptyString = "";
        assertTrue(BacktraceStringHelper.isNullOrEmpty(emptyString));
    }

    @Test
    public void testStringIsValid() {
        String someString = "foo";
        assertFalse(BacktraceStringHelper.isNullOrEmpty(someString));
    }


    @Test
    public void testObjectIsNull() {
        Object nullObject = null;
        assertFalse(BacktraceStringHelper.isObjectValidString(nullObject));
    }

    @Test
    public void testObjectIsEmptyString() {
        Object emptyString = "";
        assertFalse(BacktraceStringHelper.isObjectValidString(emptyString));
    }

    @Test
    public void testObjectIsValidString() {
        Object someString = "foo";
        assertTrue(BacktraceStringHelper.isObjectValidString(someString));
    }

    @Test
    public void testObjectIsConvertibleToString() {
        Object someString = new Integer(1);
        assertTrue(BacktraceStringHelper.isObjectValidString(someString));
    }
}
