package backtraceio.library.services;

import java.util.List;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;

import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerInput {
    public String json;
    public List<String> attachments;
    public BacktraceReport report;
    public OnServerResponseEventListener serverResponseEventListener;
    public OnServerErrorEventListener serverErrorEventListener;

    public BacktraceHandlerInput(String json, List<String>
            attachments, BacktraceReport report, OnServerResponseEventListener serverResponseEventListener, OnServerErrorEventListener serverErrorEventListener){
        this.json = json;
        this.attachments = attachments;
        this.report = report;
        this.serverResponseEventListener = serverResponseEventListener;
        this.serverErrorEventListener = serverErrorEventListener;
    }
}
