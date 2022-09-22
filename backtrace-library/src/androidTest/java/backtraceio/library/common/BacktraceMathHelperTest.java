package backtraceio.library.common;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceMathHelperTest {
    @Test
    public void testClampLow() {
        double value = 9.0;
        double minimum = 10.0;
        double maximum = 15.0;
        assertEquals(minimum, BacktraceMathHelper.clamp(value, minimum, maximum));
    }

    @Test
    public void testClampHigh() {
        double value = 16.0;
        double minimum = 10.0;
        double maximum = 15.0;
        assertEquals(maximum, BacktraceMathHelper.clamp(value, minimum, maximum));
    }

    @Test
    public void testShouldNotClamp() {
        double value = 11.0;
        double minimum = 10.0;
        double maximum = 15.0;
        BacktraceMathHelper.clamp(value, minimum, maximum);
        assertEquals(value, BacktraceMathHelper.clamp(value, minimum, maximum));
    }

    @Test
    public void testUniform() {
        double minimum = 100.0;
        double maximum = 200.0;
        for (int i = 0; i < 100; i++) {
            double value = BacktraceMathHelper.uniform(minimum, maximum);
            assertTrue(value <= maximum);
            assertTrue(value >= minimum);
        }
    }
}
