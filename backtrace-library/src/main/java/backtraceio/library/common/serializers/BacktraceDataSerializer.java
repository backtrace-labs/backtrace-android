package backtraceio.library.common.serializers;

import static backtraceio.library.common.serializers.SerializerHelper.decapitalizeString;
import static backtraceio.library.common.serializers.SerializerHelper.serialize;

import android.os.Build;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataSerializer {

    public static Map<String, Object> executeAndGetMethods(Object obj) {
        Class<?> clazz = obj.getClass();
        Map<String, Object> fields = new HashMap<>();
        Method[] methods = clazz.getMethods();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // TODO: check if needed
            for (Method method : methods) {
                String methodName = method.getName();

                if (methodName.equals("getClass")) {
                    continue;
                }

                if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                    try {
                        Object result = method.invoke(obj);
                        String propertyName = methodName.substring(3); // Remove 'get' prefix
                        fields.put(decapitalizeString(propertyName), serialize(result));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return fields;
    }

    public static String toJson(BacktraceData data) throws JSONException, IllegalAccessException {
        {
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
                final JsonArray classifiers = new JsonArray();

                for (String classifier : data.classifiers) {
                    classifiers.add(classifier);
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

            return json.toString();
        }
    }

    @NonNull
    private static JSONObject serializeThreadInformation(Map<String, ThreadInformation> threadInformationMap) throws JSONException {
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
    private static JSONArray serializeStackList(ArrayList<BacktraceStackFrame> stack) throws JSONException {
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
    private static JSONObject serializeSourceCode(Map<String, SourceCode> sourceCodeMap) throws JSONException {
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

    private static JSONObject serializeAnnotations(Map<String, Object> annotations) throws JSONException, IllegalAccessException {
        JSONObject annotationsJson = new JSONObject();
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            annotationsJson.put(entry.getKey(), serialize(entry.getValue()));
        }
        return annotationsJson;
    }

    private static JSONObject serializeAttributes(Map<String, String> attributes) throws JSONException {
        JSONObject attributesJson = new JSONObject();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attributesJson.put(entry.getKey(), entry.getValue());
        }

        return attributesJson;
    }
}
