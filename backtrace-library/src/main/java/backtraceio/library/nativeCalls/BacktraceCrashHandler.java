package backtraceio.library.nativeCalls;

import backtraceio.library.enums.UnwindingMode;

public class BacktraceCrashHandler {
    public static native boolean handleCrash(String[] args);

    public static native boolean initializeJavaCrashHandler(String url, String databasePath, String classPath, String[] attributeKeys, String[] attributeValues,
                                                            String[] attachmentPaths, String[] environmentVariables);

    public static native boolean initializeCrashHandler(String url, String databasePath, String handlerPath,
                                                        String[] attributeKeys, String[] attributeValues,
                                                        String[] attachmentPaths, boolean enableClientSideUnwinding,
                                                        UnwindingMode unwindingMode);
}