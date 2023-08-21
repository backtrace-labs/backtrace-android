package backtraceio.library.common.serializers;

import android.os.Build;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataSerializer {

    public static Object serialize(Object obj) throws IllegalAccessException, JSONException {
        if (obj == null) {
            return new JSONObject();
        }

        // TODO: check if all of the types
        if (obj instanceof String || obj instanceof Boolean || obj instanceof Number) {
            return obj;
        }

        if (obj instanceof Collection<?>) {
            return serializeCollection((Collection<?>) obj);
        }

        if (obj instanceof Map<?, ?>) {
            return serializeMap((Map<?,?>) obj);
        }

        JSONObject jsonObject = new JSONObject();
        Class<?> clazz = obj.getClass();
//        List<Field> fields = getAllFields(clazz, obj);
        Map<String, Object> getters = executeAndGetMethods(obj);
//        Map<String, Object> fields = getAllFields(obj.getClass(), obj);
//        for (Field field : fields) {
//            String fieldName = field.getName();
//            Object fieldValue = field.get(obj);
//            jsonObject.put(fieldName, fieldValue);
//        }

        return jsonObject;
    }

    public static String decapitalizeString(String string) {
        return string == null || string.isEmpty() ? "" : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    private static Map<String, Object> getAllFields(Class<?> klass, Object obj) throws IllegalAccessException {
        // TODO: improve naming
        Map<String, Object> fields = new HashMap<>();
//        List<Field> fields = new ArrayList<>();
        for (Class<?> k = klass; k != null; k = k.getSuperclass()) {
            for (Field f: k.getDeclaredFields()){
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if(val == null) {
                        continue;
                    }
                    fields.put(f.getName(), serialize(f.get(obj)));
                }
                catch (Exception e) {
                    System.out.println(e);
                }
            }
//            fields.addAll(Arrays.asList(k.getDeclaredFields()));
        }

        for (Field f: klass.getDeclaredFields()) {
            try {
                fields.put(f.getName(), serialize(f.get(obj)));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return fields;
    }

    public static Map<String, Object> executeAndGetMethods(Object obj) {
        Class<?> clazz = obj.getClass();
        Map<String, Object> fields = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            String methodName = method.getName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // TODO: check if needed
                if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                    try {
                        Object result = method.invoke(obj);
                        String propertyName = methodName.substring(3); // Remove 'get' prefix
                        System.out.println(propertyName + ": " + result);
                        fields.put(decapitalizeString(propertyName), result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return fields;
    }


    private static JSONArray serializeCollection(Collection<?> collection) throws IllegalAccessException, JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Object item : collection) {
            jsonArray.put(serialize(item));
        }
        return jsonArray;
    }

    private static JSONObject serializeMap(Map<?, ?> map) throws IllegalAccessException, JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            jsonObject.put(key, serialize(value));
        }
        return jsonObject;
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

            if(data.annotations != null) {
                json.put("annotations", serializeAnnotations(data.annotations));
            }

            if (data.sourceCode != null) {
                json.put("sourceCode", serializeSourceCode(data.sourceCode));
            }

            // Serialize threadInformationMap
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
            if(threadInfo.getStack() != null) {
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
