package backtraceio.library.coroner;


import java.util.List;

class CoronerResponse {
    public List<Object> columns;
    public List<Object> columns_desc;
    public List<Object> values;

    public CoronerResponse() {

    }

    public CoronerResponse(List<Object> columns, List<Object> columns_desc, List<Object> values) {
        this.columns = columns;
        this.columns_desc = columns_desc;
        this.values = values;
    }

    public List<Object> getColumns() {
        return columns;
    }

    public void setColumns(List<Object> columns) {
        this.columns = columns;
    }

    public List<Object> getColumns_desc() {
        return columns_desc;
    }

    public void setColumns_desc(List<Object> columns_desc) {
        this.columns_desc = columns_desc;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }
}
