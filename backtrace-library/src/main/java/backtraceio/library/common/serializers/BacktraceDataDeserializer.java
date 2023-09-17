package backtraceio.library.common.serializers;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataDeserializer {

    public static BacktraceData deserialize(Context context, JSONObject obj) throws JSONException {
        BacktraceReport report = BacktraceDataDeserializer.deserializeReport(context, obj.optJSONObject("report"));
        BacktraceData backtraceData = new BacktraceData(context, report, new HashMap<>()); // todo: fix

        backtraceData.symbolication = obj.optString("symbolication");
        backtraceData.uuid = obj.optString("uuid");
        backtraceData.timestamp = obj.optLong("timestamp");
        backtraceData.langVersion = obj.optString("langVersion");
        backtraceData.agentVersion = obj.optString("agentVersion");
        backtraceData.mainThread = obj.optString("mainThread");

        JSONArray classifiersArray = obj.optJSONArray("classifiers");
        if (classifiersArray != null) {
            backtraceData.classifiers = new String[classifiersArray.length()];
            for (int i = 0; i < classifiersArray.length(); i++) {
                backtraceData.classifiers[i] = classifiersArray.optString(i);
            }
        }

        JSONObject attributesObj = obj.optJSONObject("attributes");
        if (attributesObj != null) {
            backtraceData.attributes = new HashMap<>();
            Iterator<String> attributesKeys = attributesObj.keys();
            while (attributesKeys.hasNext()) {
                String key = attributesKeys.next();
                backtraceData.attributes.put(key, attributesObj.optString(key));
            }
        }

        JSONObject annotationsObj = obj.optJSONObject("annotations");
        if (annotationsObj != null) {
            backtraceData.annotations = new HashMap<>();
            Iterator<String> annotationsKeys = annotationsObj.keys();
            while (annotationsKeys.hasNext()) {
                String key = annotationsKeys.next();
                backtraceData.annotations.put(key, annotationsObj.opt(key));
            }
        }

        JSONObject sourceCodeObj = obj.optJSONObject("sourceCode");
        if (sourceCodeObj != null) {
            backtraceData.sourceCode = new HashMap<>();
            Iterator<String> sourceCodeKeys = sourceCodeObj.keys();
            while (sourceCodeKeys.hasNext()) {
                String key = sourceCodeKeys.next();
                JSONObject sourceCodeItem = sourceCodeObj.optJSONObject(key);
                if (sourceCodeItem != null) {
//                    SourceCode sourceCode = new SourceCode();
//                    sourceCode.startLine = sourceCodeItem.optInt("startLine");
//                    sourceCode.sourceCodeFileName = sourceCodeItem.optString("path");
//                    backtraceData.sourceCode.put(key, sourceCode);
                }
            }
        }

//        JSONObject threadInformationMapObj = obj.optJSONObject("threads");
//        if (threadInformationMapObj != null) {
//            backtraceData.threadInformationMap = new HashMap<>();
//            Iterator<String> threadKeys = threadInformationMapObj.keys();
//            while (threadKeys.hasNext()) {
//                String threadKey = threadKeys.next();
//                JSONObject threadInfoObj = threadInformationMapObj.optJSONObject(threadKey);
//                if (threadInfoObj != null) {
//                    ThreadInformation threadInformation = new ThreadInformation();
//                    threadInformation.name = threadInfoObj.optString("name");
//                    JSONArray stackArray = threadInfoObj.optJSONArray("stack");
//                    if (stackArray != null) {
//                        threadInformation.stack = new ArrayList<>();
//                        for (int i = 0; i < stackArray.length(); i++) {
//                            JSONObject stackItem = stackArray.optJSONObject(i);
//                            if (stackItem != null) {
//                                BacktraceStackFrame stackFrame = new BacktraceStackFrame();
//                                stackFrame.functionName = stackItem.optString("funcName");
//                                stackFrame.line = stackItem.optInt("line");
//                                stackFrame.sourceCode = stackItem.optString("sourceCode");
//                                threadInformation.stack.add(stackFrame);
//                            }
//                        }
//                    }
//                    backtraceData.threadInformationMap.put(threadKey, threadInformation);
//                }
//            }
//        }

        return backtraceData;
    }
    public static BacktraceReport deserializeReport(Context context, JSONObject obj) throws JSONException {
        if (obj == null ){
            return null;
        }

        BacktraceReport report = new BacktraceReport(""); // todo: fix
        report.uuid = UUID.fromString(obj.optString("uuid"));
        report.timestamp = obj.optLong("timestamp");
        report.exceptionTypeReport = obj.optBoolean("exceptionTypeReport");
        report.classifier = obj.optString("classifier");

        JSONArray attachmentPathsArray = obj.optJSONArray("attachmentPaths");
        if (attachmentPathsArray != null) {
            report.attachmentPaths = new ArrayList<>();
            for (int i = 0; i < attachmentPathsArray.length(); i++) {
                report.attachmentPaths.add(attachmentPathsArray.optString(i));
            }
        }

        report.message = obj.optString("message");

        // Deserialize exception field if needed

        JSONArray diagnosticStackArray = obj.optJSONArray("diagnosticStack");
        if (diagnosticStackArray != null) {
            report.diagnosticStack = new ArrayList<>();
            for (int i = 0; i < diagnosticStackArray.length(); i++) {
                JSONObject stackItem = diagnosticStackArray.optJSONObject(i);
                if (stackItem != null) {
                    BacktraceStackFrame stackFrame = new BacktraceStackFrame();
                    stackFrame.functionName = stackItem.optString("funcName");
                    stackFrame.line = stackItem.optInt("line");
                    stackFrame.sourceCode = stackItem.optString("sourceCode");
                    report.diagnosticStack.add(stackFrame);
                }
            }
        }

        JSONObject exceptionObj = obj.optJSONObject("exception");
        if (exceptionObj != null) {
            String exceptionClassName = exceptionObj.optString("className");
            String exceptionMessage = exceptionObj.optString("message");
            Exception exception = new Exception(exceptionMessage);
            exception.setStackTrace(parseStackFrames(exceptionObj.optJSONArray("stackTrace")));
            report.exception = exception;
        }

        return report;
    }

    public static BacktraceData deserializeReportXYZ(Context context, JSONObject obj) throws JSONException {
        BacktraceData backtraceData = new BacktraceData(context, null, null); // todo fix

        // ... (Deserialization logic for other fields) ...

        // Deserialize BacktraceReport
        JSONObject reportObj = obj.optJSONObject("report");
        if (reportObj != null) {
            BacktraceReport report = new BacktraceReport("");
            report.uuid = UUID.fromString(reportObj.optString("uuid"));
            report.timestamp = reportObj.optLong("timestamp");
            report.exceptionTypeReport = reportObj.optBoolean("exceptionTypeReport");
            report.classifier = reportObj.optString("classifier");

            JSONArray attachmentPathsArray = reportObj.optJSONArray("attachmentPaths");
            if (attachmentPathsArray != null) {
                report.attachmentPaths = new ArrayList<>();
                for (int i = 0; i < attachmentPathsArray.length(); i++) {
                    report.attachmentPaths.add(attachmentPathsArray.optString(i));
                }
            }

            report.message = reportObj.optString("message");

            // Deserialize exception field
            JSONObject exceptionObj = reportObj.optJSONObject("exception");
            if (exceptionObj != null) {
                String exceptionClassName = exceptionObj.optString("className");
                String exceptionMessage = exceptionObj.optString("message");
                Exception exception = new Exception(exceptionMessage);
                exception.setStackTrace(parseStackFrames(exceptionObj.optJSONArray("stackTrace")));
                report.exception = exception;
            }

            JSONArray diagnosticStackArray = reportObj.optJSONArray("diagnosticStack");
            if (diagnosticStackArray != null) {
                report.diagnosticStack = new ArrayList<>();
                for (int i = 0; i < diagnosticStackArray.length(); i++) {
                    JSONObject stackItem = diagnosticStackArray.optJSONObject(i);
                    if (stackItem != null) {
                        BacktraceStackFrame stackFrame = new BacktraceStackFrame();
                        stackFrame.functionName = stackItem.optString("funcName");
                        stackFrame.line = stackItem.optInt("line");
                        stackFrame.sourceCode = stackItem.optString("sourceCode");
                        report.diagnosticStack.add(stackFrame);
                    }
                }
            }

            // ... (Other deserialization for BacktraceReport fields) ...

            backtraceData.report = report;
        }

        return backtraceData;
    }

    private static StackTraceElement[] parseStackFrames(JSONArray stackTraceArray) {
        if (stackTraceArray == null) {
            return new StackTraceElement[0];
        }
        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceArray.length()];
        for (int i = 0; i < stackTraceArray.length(); i++) {
            JSONObject stackItem = stackTraceArray.optJSONObject(i);
            if (stackItem != null) {
                String className = stackItem.optString("className");
                String methodName = stackItem.optString("methodName");
                String fileName = stackItem.optString("fileName");
                int lineNumber = stackItem.optInt("lineNumber");
                stackTrace[i] = new StackTraceElement(className, methodName, fileName, lineNumber);
            }
        }
        return stackTrace;
    }

}