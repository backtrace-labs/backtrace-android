package backtraceio.coroner.serialization;

import static backtraceio.coroner.utils.ResourceUtils.EXPECTED_FRAMES;
import static backtraceio.coroner.utils.ResourceUtils.RESPONSE_RX_FILTER_CORONER_JSON;
import static backtraceio.coroner.utils.ResourceUtils.readResourceFile;
import static backtraceio.coroner.utils.StringUtils.assertJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerResponseGroup;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class GsonWrapperTest {

    private Object getResponseGroupAttributeValue(Object attribute) {
        return ((List<?>) attribute).get(0);
    }

    @Test
    public void deserializeApiResponse() throws IOException {
        // GIVEN
        final String json = readResourceFile(RESPONSE_RX_FILTER_CORONER_JSON);
        final String expectedFrames = readResourceFile(EXPECTED_FRAMES);

        // WHEN
        final CoronerApiResponse result = GsonWrapper.fromJson(json, CoronerApiResponse.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getResponse());
        assertEquals(1, result.getResponse().getResultsNumber());

        CoronerResponseGroup responseGroup = result.getResponse().values.get(0);
        assertEquals(
                "Invalid index of selected element!", getResponseGroupAttributeValue(responseGroup.getAttribute(0)));
        assertJson(
                expectedFrames,
                getResponseGroupAttributeValue(responseGroup.getAttribute(1)).toString());
        assertEquals(
                "e4c57699-0dc9-35e2-b4a0-2ffff1925ca7", getResponseGroupAttributeValue(responseGroup.getAttribute(2)));
        assertEquals(
                "java.lang.IndexOutOfBoundsException", getResponseGroupAttributeValue(responseGroup.getAttribute(3)));
    }
}
