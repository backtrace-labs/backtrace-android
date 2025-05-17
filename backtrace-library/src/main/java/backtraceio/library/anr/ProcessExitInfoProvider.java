package backtraceio.library.anr;

import java.util.List;

public interface ProcessExitInfoProvider {
    List<ExitInfo> getHistoricalExitInfo(String packageName, int startIndex, int maxCount);

    List<Integer> getSupportedTypesOfExitInfo();
}
