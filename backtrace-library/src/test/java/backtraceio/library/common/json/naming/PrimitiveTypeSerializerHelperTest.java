package backtraceio.library.common.json.naming;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import backtraceio.library.common.json.serialization.SerializerHelper;

@RunWith(Parameterized.class)
public class PrimitiveTypeSerializerHelperTest {
    private final Object testedObj;
    private final boolean expectedResult;

    // Constructor for the parameterized test
    public PrimitiveTypeSerializerHelperTest(Object testedObj, boolean expectedResult) {
        this.testedObj = testedObj;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, true},             // int
                {1L, true},            // long
                {1.0f, true},          // float
                {'c', true},           // char
                {"test", true},        // string
                {1.5, true},           // double
                {new Object(), false}, // object
                {new Exception("test"), false}, // object exception
                {new ArrayList<>(), false}, // array list
                {new HashMap<>(), false} // hash map
        });
    }
    @Test
    public void testIsPrimitiveType() {
        // WHEN
        boolean result = SerializerHelper.isPrimitiveType(testedObj);

        // THEN
        assertEquals(expectedResult, result);
    }
}
