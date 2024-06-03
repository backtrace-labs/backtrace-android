package backtraceio.library.interfaces;

import backtraceio.library.enums.UnwindingMode;

public interface NativeCommunication {
    boolean handleCrash(String[] args);

    boolean initializeJavaCrashHandler(String url, String databasePath, String classPath, String[] attributeKeys, String[] attributeValues,
                                       String[] attachmentPaths, String[] environmentVariables);

    boolean initializeCrashHandler(String url, String databasePath, String handlerPath,
                                   String[] attributeKeys, String[] attributeValues,
                                   String[] attachmentPaths, boolean enableClientSideUnwinding,
                                   UnwindingMode unwindingMode);
}
