package backtraceio.library.anr;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExitInfoStackTraceParser {

    public static StackTraceElement parseNativeFrame(String frame) {
        return null;
    }
    public static StackTraceElement[] parseMainThreadStackTrace(Map<String, Object> parsedData) {
        Map<String, Object> mainThreadInfo = (Map<String, Object>) parsedData.get("main_thread");

        if (mainThreadInfo != null) {
            List<String> stackFrames = (List<String>) mainThreadInfo.get("stack_trace");
            if (stackFrames != null) {
                List<StackTraceElement> elements = new ArrayList<>();
                Pattern javaFramePattern = Pattern.compile("\\s*at (.*?)\\((.*?):(\\d+)\\)");
                for (String frame : stackFrames) {
                    Matcher matcher = javaFramePattern.matcher(frame);
                    if (matcher.find()) {
                        String fullClassNameMethod = matcher.group(1);
                        String fileName = matcher.group(2);
                        int lineNumber = Integer.parseInt(matcher.group(3));

                        int lastDot = fullClassNameMethod.lastIndexOf('.');
                        String className = (lastDot == -1) ? fullClassNameMethod : fullClassNameMethod.substring(0, lastDot);
                        String methodName = (lastDot == -1) ? "" : fullClassNameMethod.substring(lastDot + 1);

                        elements.add(new StackTraceElement(className, methodName, fileName, lineNumber));
                    }
                    else {
                        if (!frame.startsWith("native")) {
                            continue;
                        }
                        String[] parts = frame.split("\\s+", 6);

                        String address = parts[3]; // "0005ad68"
                        String library = parts[4]; // "/apex/com.android.runtime/lib/bionic/libc.so"
                        String funcName = parts[5].substring(1, parts[5].indexOf('+')); // strip "(" and extract up to "+"

                        Map<String, String> result = new HashMap<>();
                        result.put("funcName", funcName);
                        result.put("library", library);
                        result.put("address", address);;

                        elements.add(new StackTraceElement("native: " + library, funcName, " address: " + address, -1));
//                        elements.add(new StackTraceElement(library, funcName,  address, 0));
                    }
                }
                return elements.toArray(new StackTraceElement[0]);
            }
        }
        return new StackTraceElement[0];
    }

    public static Map<String, Object> parseStackTrace(String stackTrace) {
        Map<String, Object> parsedData = new HashMap<>();

        if(stackTrace == null || stackTrace.isEmpty()) {
            return parsedData;
        }

        // Parse timestamp
        Pattern timestampPattern = Pattern.compile("----- pid \\d+ at (.*?) -----");
        Matcher timestampMatcher = timestampPattern.matcher(stackTrace);
        if (timestampMatcher.find()) {
            String timestampStr = timestampMatcher.group(1);
            try {
                DateTimeFormatter formatter = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                LocalDateTime dateTime = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    dateTime = LocalDateTime.parse(timestampStr, formatter);
                }
                parsedData.put("timestamp", dateTime);
            } catch (Exception e) {
                parsedData.put("timestamp_raw", timestampStr);
            }
        }

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
        List<Map<String, Object>> threads = new ArrayList<>();
        Pattern threadStartPattern = Pattern.compile("\"(.*?)\" (daemon )?prio=(\\d+) tid=(\\d+) (.*?)\n\\s*\\| group=\"(.*?)\" sCount=(\\d+) dsCount=(\\d+) flags=(\\d+) obj=(.*?) self=(.*?)\n\\s*\\| sysTid=(\\d+) nice=(-?\\d+) cgrp=(.*?) sched=(.*?)/.*? handle=(.*?)");
        Matcher threadStartMatcher = threadStartPattern.matcher(stackTrace);

        while (threadStartMatcher.find()) {
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("name", threadStartMatcher.group(1));
            threadInfo.put("daemon", threadStartMatcher.group(2) != null);
            threadInfo.put("priority", Integer.parseInt(threadStartMatcher.group(3)));
            threadInfo.put("tid", Integer.parseInt(threadStartMatcher.group(4)));
            threadInfo.put("state", threadStartMatcher.group(5).trim());
            threadInfo.put("group", threadStartMatcher.group(6));
            threadInfo.put("scount", Integer.parseInt(threadStartMatcher.group(7)));
            threadInfo.put("dscount", Integer.parseInt(threadStartMatcher.group(8)));
            threadInfo.put("flags", Integer.parseInt(threadStartMatcher.group(9)));
            threadInfo.put("obj", threadStartMatcher.group(10));
            threadInfo.put("self", threadStartMatcher.group(11));
            threadInfo.put("systid", Integer.parseInt(threadStartMatcher.group(12)));
            threadInfo.put("nice", Integer.parseInt(threadStartMatcher.group(13)));
            threadInfo.put("cgrp", threadStartMatcher.group(14));
            threadInfo.put("sched", threadStartMatcher.group(15));
            threadInfo.put("handle", threadStartMatcher.group(16));

            // Parse the stack trace for this thread
            StringBuilder stackTraceBuilder = new StringBuilder();
            Pattern stackFramePattern = Pattern.compile("\\s*(native: #\\d+ pc .*|at .*\\((.*?)\\))");
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
            threadInfo.put("stack_trace", stackFrames);
            threads.add(threadInfo);
        }
        parsedData.put("threads", threads);

        // Find the main thread
        Map<String, Object> mainThreadInfo = null;
        for (Map<String, Object> thread : threads) {
            if (thread.get("name").equals("main")) {
                mainThreadInfo = thread;
                break;
            }
        }
        parsedData.put("main_thread", mainThreadInfo);

        return parsedData;
    }
}
