package backtraceio.coroner.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResourceUtils {
    private static final String MAIN_PATH = "src/test/resources/backtraceio/coroner";
    public static final String QUERY_CORONER_RXID_123 = MAIN_PATH + "/queries/rxid-12345.json";
    public static final String QUERY_CORONER_RXID_123_ATTR_ERR_MSG = MAIN_PATH + "/queries/rxid-12345-custom-attr-err-msg.json";
    public static final String QUERY_CORONER_TIMESTAMP_ERR_TYPE= MAIN_PATH + "/queries/timestamp-err-type-filter.json";
    public static final String RESPONSE_RX_FILTER_CORONER_JSON = MAIN_PATH + "/responses/rx-filter-response.json";
    public static final String EXPECTED_FRAMES = MAIN_PATH + "/frames.json";
    public static final String RESPONSE_TIMESTAMP_ERR_TYPE_CORONER_JSON = MAIN_PATH + "/responses/timestamp-err-type-filter.json";

    public static final String RESPONSE_OPERATION_ERROR_JSON = MAIN_PATH + "/responses/operation-error.json";

    public static String readResourceFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes);
    }

}
