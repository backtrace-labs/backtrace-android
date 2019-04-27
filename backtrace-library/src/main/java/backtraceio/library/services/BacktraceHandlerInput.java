package backtraceio.library.services;

import java.util.List;
import java.util.UUID;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerInput {
    public UUID requestId;
    public String json;
    public List<String> attachments;
    public BacktraceReport report;

    public BacktraceHandlerInput(UUID requestId, String json, List<String>
            attachments, BacktraceReport report){
        this.requestId = requestId;
        this.json = json;
        this.attachments = attachments;
        this.report = report;
    }
}
