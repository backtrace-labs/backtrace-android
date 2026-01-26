package backtraceio.library.nativeCalls;

import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.interfaces.NativeCommunication;

public class BacktraceCrashHandlerWrapper implements NativeCommunication {
    public boolean handleCrash(String[] args) {
        return BacktraceCrashHandler.handleCrash(args);
    }

    public boolean initializeJavaCrashHandler(
            String url,
            String databasePath,
            String classPath,
            String[] attributeKeys,
            String[] attributeValues,
            String[] attachmentPaths,
            String[] environmentVariables) {
        return BacktraceCrashHandler.initializeJavaCrashHandler(
                url, databasePath, classPath, attributeKeys, attributeValues, attachmentPaths, environmentVariables);
    }

    public boolean initializeCrashHandler(
            String url,
            String databasePath,
            String handlerPath,
            String[] attributeKeys,
            String[] attributeValues,
            String[] attachmentPaths,
            boolean enableClientSideUnwinding,
            UnwindingMode unwindingMode) {
        return BacktraceCrashHandler.initializeCrashHandler(
                url,
                databasePath,
                handlerPath,
                attributeKeys,
                attributeValues,
                attachmentPaths,
                enableClientSideUnwinding,
                unwindingMode);
    }
}
