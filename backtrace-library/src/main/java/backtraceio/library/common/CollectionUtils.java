package backtraceio.library.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
    public static <T> List<T> copyList(List<T> userList) {
        List<T> copiedList = new ArrayList<>();

        if (userList != null) {
            copiedList.addAll(userList);
        }

        return copiedList;
    }

    public static <K, V> Map<K, V> copyMap(Map<K, V> userMap) {
        HashMap<K, V> copiedMap = new HashMap<>();

        if (userMap != null) {
            copiedMap.putAll(userMap);
        }

        return copiedMap;
    }

    public static <T> Set<T> copySet(Set<T> userSet) {
        HashSet<T> copiedSet = new HashSet<>();

        if (userSet != null) {
            copiedSet.addAll(userSet);

        }
        return copiedSet;
    }
}
