package backtraceio.library.anr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExitInfoStackTraceParser {
    private static final Pattern JAVA_FRAME_PATTERN = Pattern.compile("\\s*at (.*?)\\((.*?):(\\d+)\\)");
    private static final String MAIN_THREAD_NAME = "main";
    private static final int NATIVE_STACK_ELEMENTS_NUMBER = 6;
    static StackTraceElement parseFrame(String frame) {
        StackTraceElement javaFrame = parseJavaFrame(frame);
        if (javaFrame != null) {
            return javaFrame;
        }

        return parseNativeFrame(frame);
    }

    static StackTraceElement parseNativeFrame(String frame) {
        if (!frame.startsWith("native")) {
            return null;
        }
        String[] parts = frame.split("\\s+", NATIVE_STACK_ELEMENTS_NUMBER);

        if (parts.length < NATIVE_STACK_ELEMENTS_NUMBER) {
            return null;
        }

        String address = parts[3];
        String library = parts[4];
        String funcName = parts[5];

        return new StackTraceElement(library, funcName, "address: " + address, 0);
    }

    static StackTraceElement parseJavaFrame(String frame) {
        Matcher matcher = JAVA_FRAME_PATTERN.matcher(frame);
        if (!matcher.find()) {
            return null;
        }

        String fullClassNameMethod = matcher.group(1);
        String fileName = matcher.group(2);
        int lineNumber = Integer.parseInt(matcher.group(3));

        int lastDot = fullClassNameMethod.lastIndexOf('.');
        String className = (lastDot == -1) ? fullClassNameMethod : fullClassNameMethod.substring(0, lastDot);
        String methodName = (lastDot == -1) ? "" : fullClassNameMethod.substring(lastDot + 1);

        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    public static StackTraceElement[] parseMainThreadStackTrace(Map<String, Object> parsedData) {
        Map<String, Object> mainThreadInfo = (Map<String, Object>) parsedData.get("main_thread");

        if (mainThreadInfo == null) {
            return new StackTraceElement[0];
        }

        List<String> stackFrames = (List<String>) mainThreadInfo.get("stack_trace");

        if (stackFrames == null) {
            return new StackTraceElement[0];
        }

        List<StackTraceElement> elements = new ArrayList<>();
        for (String frame : stackFrames) {
            StackTraceElement element = parseFrame(frame);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.toArray(new StackTraceElement[0]);
    }

    public static Map<String, Object> parseANRStackTrace(String stackTrace) {
        Map<String, Object> parsedData = new HashMap<>();

        if (stackTrace == null || stackTrace.isEmpty()) {
            return parsedData;
        }

            // Parse timestamp
        parsedData.put("timestamp", parseTimestamp(stackTrace));

        // Parse PID
        Pattern pidPattern = Pattern.compile("----- pid (\\d+) at");
        Matcher pidMatcher = pidPattern.matcher(stackTrace);
        if (pidMatcher.find()) {
            parsedData.put("pid", Integer.parseInt(pidMatcher.group(1)));
        }

        // Parse command line
        Pattern cmdLinePattern = Pattern.compile("Cmd line: (.*)");
        Matcher cmdLineMatcher = cmdLinePattern.matcher(stackTrace);
        if (cmdLineMatcher.find()) {
            parsedData.put("command_line", cmdLineMatcher.group(1));
        }

        // Parse build fingerprint
        Pattern fingerprintPattern = Pattern.compile("Build fingerprint: '(.*?)'");
        Matcher fingerprintMatcher = fingerprintPattern.matcher(stackTrace);
        if (fingerprintMatcher.find()) {
            parsedData.put("build_fingerprint", fingerprintMatcher.group(1));
        }

        // Parse ABI
        Pattern abiPattern = Pattern.compile("ABI: '(.*?)'");
        Matcher abiMatcher = abiPattern.matcher(stackTrace);
        if (abiMatcher.find()) {
            parsedData.put("abi", abiMatcher.group(1));
        }

        // Parse build type
        Pattern buildTypePattern = Pattern.compile("Build type: (.*)");
        Matcher buildTypeMatcher = buildTypePattern.matcher(stackTrace);
        if (buildTypeMatcher.find()) {
            parsedData.put("build_type", buildTypeMatcher.group(1));
        }

        // Parse heap information
        Pattern heapPattern = Pattern.compile("Heap: (.*)");
        Matcher heapMatcher = heapPattern.matcher(stackTrace);
        if (heapMatcher.find()) {
            parsedData.put("heap_info", heapMatcher.group(1));
        }

        // Parse Dalvik Threads

//        Pattern threadStartPattern = Pattern.compile(
//                "\"(.*?)\" (daemon )?prio=(\\d+) tid=(\\d+) (.*?)\n\\s*\\| group=\"(.*?)\" sCount=(\\d+) dsCount=(\\d+) flags=(\\d+) obj=(.*?) self=(.*?)\n\\s*\\| sysTid=(\\d+) nice=(-?\\d+) cgrp=(.*?) sched=(.*?) handle=(.*?)");
//
//        Matcher threadStartMatcher = threadStartPattern.matcher(stackTrace);
//
//        while (threadStartMatcher.find()) {
//            Map<String, Object> threadInfo = ;
//            threads.add(threadInfo);
//        }
        List<Map<String, Object>> threads = parseThreadDumps(stackTrace);
        parsedData.put("threads", threads);

        // Find the main thread
        Map<String, Object> mainThreadInfo = getMainThreadInfo(threads);
        parsedData.put("main_thread", mainThreadInfo);

        return parsedData;
    }

    private static Object parseTimestamp(String stackTrace) {
        Pattern timestampPattern = Pattern.compile("----- pid \\d+ at (.*?) -----");
        Matcher timestampMatcher = timestampPattern.matcher(stackTrace);
        if (timestampMatcher.find()) {
            return timestampMatcher.group(1);
        }
        return null;
    }

    @Nullable
    private static Map<String, Object> getMainThreadInfo(List<Map<String, Object>> threads) {
        Map<String, Object> mainThreadInfo = null;
        for (Map<String, Object> thread : threads) {
            if (thread.get("name").equals(MAIN_THREAD_NAME)) {
                mainThreadInfo = thread;
                break;
            }
        }
        return mainThreadInfo;
    }
    private static Map<String, Object> parseThreadInformation(String threadDump) {
        Map<String, Object> result = new HashMap<>();

        // Parse header line
        Pattern headerPattern = Pattern.compile("\"([^\"]+)\"\\s*(daemon)?\\s*prio=(\\d+)\\s*tid=(\\d+)\\s*([^\\n]+)");
        Matcher headerMatcher = headerPattern.matcher(threadDump);

        if (headerMatcher.find()) {
            result.put("name", headerMatcher.group(1));
            result.put("isDaemon", headerMatcher.group(2) != null);
            result.put("priority", Integer.parseInt(headerMatcher.group(3)));
            result.put("tid", Integer.parseInt(headerMatcher.group(4)));
            result.put("status", headerMatcher.group(5).trim());
        }

        // Parse info lines
        List<String> stackTrace = new ArrayList<>();
        String[] lines = threadDump.split("\n");
        boolean isStackTrace = false;

        for (String line : lines) {
            line = line.trim();

            if (isStackTrace && (line.isEmpty() || line.startsWith("\""))) {
                break;
            }

            // Skip empty lines and first line (already parsed)
            if (line.isEmpty() || line.startsWith("\"")) {
                continue;
            }

            // Check if we've reached stack trace
            if (line.startsWith("at ") || line.startsWith("native:")) {
                isStackTrace = true;
                stackTrace.add(line);
                continue;
            }

            if (isStackTrace) {
                continue;
            }

            // Parse info lines (starting with |)
            if (line.startsWith("|")) {
                line = line.substring(1).trim(); // Remove the | character
                String[] pairs = line.split("\\s+");

                for (String pair : pairs) {
                    if (pair.contains("=")) {
                        String[] keyValue = pair.split("=", 2);
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        // Remove quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }

                        // Try to parse numbers
                        try {
                            if (value.matches("-?\\d+")) {
                                result.put(key, Integer.parseInt(value));
                            } else {
                                result.put(key, value);
                            }
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    }
                }
            }
        }

        // Add stack trace if we found any
        if (!stackTrace.isEmpty()) {
            result.put("stack_trace", stackTrace);
        }

        return result;
    }

    public static List<Map<String, Object>> parseThreadDumps(String input) {
        List<Map<String, Object>> threads = new ArrayList<>();

        // Split input into individual thread dumps
        // Pattern matches the start of each thread dump (looking for quoted thread name)
//        String regex = "\\\"(.*?)\\\" prio=(\\\\d+) tid=(\\\\d+) (\\\\w+)\\\\n(?s)(.*?)(?=(?:\\\\n\\\\n)|$)";
        String regex = "\\\"(.*?)\\\" (daemon )?prio=(\\d+) tid=(\\d+) (\\w+)(.*)\\n(?s)((.*?\\n))(?=(?:\\n\\n)|$)";

        Pattern threadStartPattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher threadMatcher = threadStartPattern.matcher(input);

        while (threadMatcher.find()) {
            String threadDump = threadMatcher.group();
            Map<String, Object> threadInfo = parseThreadInformation(threadDump);
            threads.add(threadInfo);
        }

        return threads;
    }
//    @NonNull
//    private static Map<String, Object> parseThreadInformation(String stackTrace, Matcher threadStartMatcher) {
//        Map<String, Object> threadInfo = new HashMap<>();
//        threadInfo.put("name", threadStartMatcher.group(1));
//        threadInfo.put("daemon", threadStartMatcher.group(2) != null);
//        threadInfo.put("priority", Integer.parseInt(threadStartMatcher.group(3)));
//        threadInfo.put("tid", Integer.parseInt(threadStartMatcher.group(4)));
//        threadInfo.put("state", threadStartMatcher.group(5).trim());
//        threadInfo.put("group", threadStartMatcher.group(6));
//        threadInfo.put("scount", Integer.parseInt(threadStartMatcher.group(7)));
//        threadInfo.put("dscount", Integer.parseInt(threadStartMatcher.group(8)));
//        threadInfo.put("flags", Integer.parseInt(threadStartMatcher.group(9)));
//        threadInfo.put("obj", threadStartMatcher.group(10));
//        threadInfo.put("self", threadStartMatcher.group(11));
//        threadInfo.put("systid", Integer.parseInt(threadStartMatcher.group(12)));
//        threadInfo.put("nice", Integer.parseInt(threadStartMatcher.group(13)));
//        threadInfo.put("cgrp", threadStartMatcher.group(14));
//        threadInfo.put("sched", threadStartMatcher.group(15));
//        threadInfo.put("handle", threadStartMatcher.group(16));
//        threadInfo.put("stack_trace", parseThreadStacktrace(stackTrace, threadStartMatcher));
//        return threadInfo;
//    }

    @NonNull
    private static List<String> parseThreadStacktrace(String stackTrace, Matcher threadStartMatcher) {
        Pattern stackFramePattern = Pattern.compile("\\s*(native: #\\d+ pc .*|at .*\\((.*?)\\))"); // native or java stack frame
        int startIndex = threadStartMatcher.end();
        int endIndex = stackTrace.indexOf("\n\"", startIndex); // Find the start of the next thread or end of input
        if (endIndex == -1) {
            endIndex = stackTrace.length();
        }
        Matcher stackFrameMatcher = stackFramePattern.matcher(stackTrace.substring(startIndex, endIndex));
        List<String> stackFrames = new ArrayList<>();
        while (stackFrameMatcher.find()) {
            stackFrames.add(stackFrameMatcher.group().trim());
        }
        return stackFrames;
    }
}
