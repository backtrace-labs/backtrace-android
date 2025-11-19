package backtraceio.coroner.response;

public class ColumnDescElement {
    public String name;
    public String format;
    public String type;
    public String op;

    @SuppressWarnings("unused")
    public ColumnDescElement() {}

    @SuppressWarnings("unused")
    public ColumnDescElement(String name, String format, String type, String op) {
        this.name = name;
        this.format = format;
        this.type = type;
        this.op = op;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unused")
    public String getFormat() {
        return format;
    }

    @SuppressWarnings("unused")
    public void setFormat(String format) {
        this.format = format;
    }

    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    public String getOp() {
        return op;
    }

    @SuppressWarnings("unused")
    public void setOp(String op) {
        this.op = op;
    }
}
