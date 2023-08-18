package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.BacktraceConstants;
import backtraceio.library.models.json.BacktraceReport;

public abstract class BacktraceBaseData {
    /**
     * Name of the client that is sending this error report.
     */
    @SerializedName("agent")
    public final String agent = "backtrace-android";

    /**
     * Version of the android library
     */
    @SerializedName("agentVersion")
    public String agentVersion = BacktraceClient.version;

    /**
     * Get built-in attributes
     */
    @SerializedName("attributes")
    public Map<String, String> attributes;

    /**
     * Current BacktraceReport
     */
    public transient BacktraceReport report;

    /**
     * Get absolute paths to report attachments
     *
     * @return paths to attachments
     */
    public abstract List<String> getAttachments();

    /**
     * Check if there is a minidump amongst the attachments
     * @return if there is a minidump contained in the BacktraceData attachments
     */
    public boolean containsMinidump() {
        for(String attachment: this.getAttachments()) {
            if (attachment.endsWith(BacktraceConstants.MinidumpExtension)) {
                return true;
            }
        }
        return false;
    }

}
