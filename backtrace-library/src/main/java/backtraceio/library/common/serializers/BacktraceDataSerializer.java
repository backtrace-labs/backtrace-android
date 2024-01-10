package backtraceio.library.common.serializers;

import static backtraceio.library.common.serializers.SerializerHelper.serialize;

import androidx.annotation.NonNull;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;
import backtraceio.library.common.serializers.naming.NamingPolicy;

public class BacktraceDataSerializer {
    NamingPolicy namingPolicy;
    public BacktraceDataSerializer(NamingPolicy policy) {
        namingPolicy = policy;
    }


    public JSONObject toJson(BacktraceData data) throws JSONException, IllegalAccessException {
        {
            if (data == null) {
                return null;
            }

            JSONObject json = new JSONObject();

            // Serialize simple fields
            json.put("lang", data.lang);
            json.put("agent", data.agent);
            json.put("symbolication", data.symbolication);
            json.put("uuid", data.uuid);
            json.put("timestamp", data.timestamp);
            json.put("langVersion", data.langVersion);
            json.put("agentVersion", data.agentVersion);
            json.put("mainThread", data.mainThread);

            if (data.classifiers != null) {
                final JSONArray classifiers = new JSONArray();
                for (String classifier : data.classifiers) {
                    classifiers.put(classifier);
                }
                json.put("classifiers", classifiers);
            }

            if (data.attributes != null) {
                json.put("attributes", serializeAttributes(data.attributes));
            }

            if (data.annotations != null) {
                json.put("annotations", serializeAnnotations(data.annotations));
            }

            if (data.sourceCode != null) {
                json.put("sourceCode", serializeSourceCode(data.sourceCode));
            }

            if (data.getThreadInformationMap() != null) {
                JSONObject threadInformationJson = serializeThreadInformation(data.getThreadInformationMap());
                json.put("threads", threadInformationJson);
            }

            return json;
        }
    }

    @NonNull
    private JSONObject serializeThreadInformation(Map<String, ThreadInformation> threadInformationMap) throws JSONException {
        JSONObject threadInformationJson = new JSONObject();
        for (Map.Entry<String, ThreadInformation> entry : threadInformationMap.entrySet()) {
            ThreadInformation threadInfo = entry.getValue();
            JSONObject threadInfoObj = new JSONObject();
            threadInfoObj.put("name", threadInfo.getName());
            threadInfoObj.put("fault", threadInfo.getFault());
            if (threadInfo.getStack() != null) {
                JSONArray stackArray = serializeStackList(threadInfo.getStack());
                threadInfoObj.put("stack", stackArray);
            }
            threadInformationJson.put(entry.getKey(), threadInfoObj);
        }
        return threadInformationJson;
    }

    @NonNull
    private JSONArray serializeStackList(List<BacktraceStackFrame> stack) throws JSONException {
        JSONArray stackArray = new JSONArray();

        for (BacktraceStackFrame stackFrame : stack) {
            JSONObject stackFrameObj = new JSONObject();
            stackFrameObj.put("funcName", stackFrame.functionName);
            stackFrameObj.put("line", stackFrame.line);
            stackFrameObj.put("sourceCode", stackFrame.sourceCode);
            stackArray.put(stackFrameObj);
        }

        return stackArray;
    }

    @NonNull
    private JSONObject serializeSourceCode(Map<String, SourceCode> sourceCodeMap) throws JSONException {
        JSONObject sourceCodeJson = new JSONObject();
        for (Map.Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
            SourceCode sourceCode = entry.getValue();
            JSONObject sourceCodeObj = new JSONObject();
            sourceCodeObj.put("startLine", sourceCode.startLine);
            sourceCodeObj.put("path", sourceCode.sourceCodeFileName);
            sourceCodeJson.put(entry.getKey(), sourceCodeObj);
        }
        return sourceCodeJson;
    }

    private JSONObject serializeAnnotations(Map<String, Object> annotations) throws JSONException, IllegalAccessException {
        JSONObject annotationsJson = new JSONObject();
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            annotationsJson.put(entry.getKey(), serialize(this.namingPolicy, entry.getValue()));
        }
        return annotationsJson;
    }

    private JSONObject serializeAttributes(Map<String, String> attributes) throws JSONException {
        JSONObject attributesJson = new JSONObject();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attributesJson.put(entry.getKey(), entry.getValue());
        }

        return attributesJson;
    }
}
