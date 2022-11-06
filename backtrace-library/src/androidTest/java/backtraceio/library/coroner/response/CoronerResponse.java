package backtraceio.library.coroner.response;


import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.coroner.serialization.CoronerResponseGroupDeserializer;
import backtraceio.library.logger.BacktraceLogger;

public class CoronerResponse {

    private static final transient String LOG_TAG = CoronerResponseGroupDeserializer.class.getSimpleName();

    @SerializedName(value = "columns_desc")
    public List<ColumnDescElement> columnsDesc;
    public List<CoronerResponseGroup> values;

    @SuppressWarnings("unused")
    public CoronerResponse() {
    }

    @SuppressWarnings("unused")
    public CoronerResponse(List<ColumnDescElement> columnsDesc, List<CoronerResponseGroup> values) {
        this.columnsDesc = columnsDesc;
        this.values = values;
    }

    public <T> T getAttribute(int elementIndex, String name, Class<T> clazz) throws CoronerResponseProcessingException {
        if (this.values == null) {
            throw new CoronerResponseProcessingException("Values property from response is null");
        }
        if (elementIndex < 0 || elementIndex > this.values.size()) {
            throw new CoronerResponseProcessingException("Incorrect element index, value should be between 0 and " + this.values.size());
        }
        CoronerResponseGroup responseGroup = values.get(elementIndex);

        int attributeIndex = getAttributeIndex(name);
        try {
            List<Object> results = (ArrayList<Object>) responseGroup.getAttribute(attributeIndex);
            return clazz.cast(results.get(0));
        } catch (ClassCastException e) {
            BacktraceLogger.e(LOG_TAG, e.getMessage());
            throw new CoronerResponseProcessingException("Error on getting attribute from response group for attribute index: " + attributeIndex);
        }
    }

    public int getResultsNumber() {
        return values.size();
    }

    private int getAttributeIndex(String attributeName) throws CoronerResponseProcessingException {
        for (int index = 0; index < this.columnsDesc.size(); index++) {
            if (this.columnsDesc.get(index).name.equals(attributeName)) {
                return index;
            }
        }
        throw new CoronerResponseProcessingException("Attribute not found for name " + attributeName);
    }

}
