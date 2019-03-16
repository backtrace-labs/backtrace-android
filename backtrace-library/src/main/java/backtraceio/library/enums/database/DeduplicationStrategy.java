package backtraceio.library.enums.database;

public enum DeduplicationStrategy {
    /**
     * Ignore deduplication strategy
     */
    None(0),
    /**
     * Only stack trace
     */
    Default(1),
    /**
     * Stack trace and exception type
     */
    Classifier(2),
    /**
     * Stack trace and exception message
     */
    Message(4),
    /**
     * Stack trace and library name
     */
    LibraryName(8);

    int flag;
    DeduplicationStrategy(int v){
        this.flag = v;
    }

    int getFlag(){
        return flag;
    }
}