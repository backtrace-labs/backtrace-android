package backtraceio.coroner.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoronerResponse {

    private static final Logger LOGGER = Logger.getLogger(CoronerResponse.class.getName());

    @SerializedName(value = "columns_desc")
    public List<ColumnDescElement> columnsDesc;

    @SerializedName(value = "values")
    public List<CoronerResponseGroup> values;

    @SuppressWarnings("unused")
    public CoronerResponse() {
    }

    @SuppressWarnings("unused")
    public CoronerResponse(final List<ColumnDescElement> columnsDesc, final List<CoronerResponseGroup> values) {
        this.columnsDesc = columnsDesc;
        this.values = values;
    }

    public <T> T getAttribute(final int elementIndex, final String name, final Class<T> clazz) throws CoronerResponseProcessingException {
        if (this.values == null) {
            throw new CoronerResponseProcessingException("Values property from response is null");
        }
        if (elementIndex < 0 || elementIndex > this.values.size()) {
            throw new CoronerResponseProcessingException("Incorrect element index, value should be between 0 and " + this.values.size());
        }
        final CoronerResponseGroup responseGroup = values.get(elementIndex);

        final int attributeIndex = getAttributeIndex(name);
        try {
            final List<Object> results = (ArrayList<Object>) responseGroup.getAttribute(attributeIndex);
            return clazz.cast(results.get(0));
        } catch (ClassCastException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new CoronerResponseProcessingException("Error on getting attribute from response group for attribute index: " + attributeIndex);
        }
    }

    public int getResultsNumber() {
        return values.size();
    }

    private int getAttributeIndex(final String attributeName) throws CoronerResponseProcessingException {
        for (int index = 0; index < this.columnsDesc.size(); index++) {
            if (this.columnsDesc.get(index).name.equals(attributeName)) {
                return index;
            }
        }
        throw new CoronerResponseProcessingException("Attribute not found for name " + attributeName);
    }

}
