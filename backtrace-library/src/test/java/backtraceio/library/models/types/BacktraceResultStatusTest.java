package backtraceio.library.models.types;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class BacktraceResultStatusTest {
    @RunWith(Parameterized.class)
    public static class BacktraceResultStatusParametrizedTests {
        private final String apiStatus;
        private final BacktraceResultStatus expectedApiStatusEnum;

        public BacktraceResultStatusParametrizedTests(String apiStatus, BacktraceResultStatus expectedApiStatusEnum) {
            this.apiStatus = apiStatus;
            this.expectedApiStatusEnum = expectedApiStatusEnum;
        }

        @Parameterized.Parameters
        public static Collection apiStringStatus() {
            return Arrays.asList(new Object[][] {
                {"ok", BacktraceResultStatus.Ok},
                {"Ok", BacktraceResultStatus.Ok},
                {"OK", BacktraceResultStatus.Ok},
                {"oK", BacktraceResultStatus.Ok},
                {"servererror", BacktraceResultStatus.ServerError},
                {"serverError", BacktraceResultStatus.ServerError},
                {"ServerError", BacktraceResultStatus.ServerError},
                {"SERVERERROR", BacktraceResultStatus.ServerError}
            });
        }

        @Test
        public void testMappingStatusStringToEnum() {
            assertEquals(this.expectedApiStatusEnum, BacktraceResultStatus.enumOf(this.apiStatus));
        }
    }

    public static class BacktraceResultStatusSingleTests {

        @Test(expected = IllegalArgumentException.class)
        public void testWrongStatusValue() {
            BacktraceResultStatus.enumOf("unsupported-value");
        }
    }
}
