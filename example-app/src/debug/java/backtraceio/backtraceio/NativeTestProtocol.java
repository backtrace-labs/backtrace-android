package backtraceio.backtraceio;

/**
 * Messenger protocol between instrumentation and the debug-only native qualification services.
 * Primitive-only bundles: strings, ints, booleans, and longs. Credentials are never sent in either
 * direction; database and handler paths travel only in controlled test responses and artifacts.
 */
final class NativeTestProtocol {
    static final int CMD_PING = 1;
    static final int CMD_RUN_FRESH_PROCESS_SAFETY = 2;
    static final int CMD_PREPARE_REAL_NATIVE = 3;
    static final int CMD_PREPARE_RECOVERY = 4;
    static final int CMD_DUMP = 5;
    static final int CMD_DISABLE = 6;
    static final int CMD_ENABLE = 7;
    static final int CMD_CRASH = 8;
    static final int CMD_CLEANUP = 9;

    static final int EVENT_READY = 100;
    static final int EVENT_COMPLETED = 101;
    static final int EVENT_WILL_CRASH = 102;
    static final int EVENT_FAILED = 199;

    static final String KEY_GUID = "guid";
    static final String KEY_MESSAGE = "message";
    static final String KEY_DATABASE_PATH = "database_path";
    static final String KEY_HANDLER_PATH = "handler_path";
    static final String KEY_PROCESS_ABI = "process_abi";
    static final String KEY_IS_64_BIT = "is_64_bit";
    static final String KEY_PID = "pid";
    static final String KEY_ERROR_TYPE = "error_type";
    static final String KEY_SCENARIO = "scenario";

    private NativeTestProtocol() {}
}
