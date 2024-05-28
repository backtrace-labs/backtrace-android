package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtilsTest {

    @Test
    public void testCopyList_NullList() {
        // WHEN
        List<String> result = CollectionUtils.copyList(null);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCopyList_EmptyList() {
        // GIVEN
        List<String> emptyList = new ArrayList<>();
        // WHEN
        List<String> result = CollectionUtils.copyList(emptyList);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCopyList_NonEmptyList() {
        // GIVEN
        List<String> userList = Arrays.asList("one", "two", "three");

        // WHEN
        List<String> result = CollectionUtils.copyList(userList);

        // THEN
        assertNotNull(result);
        assertEquals(userList.size(), result.size());
        assertTrue(result.containsAll(userList));
    }

    @Test
    public void testCopyMap_NullMap() {
        // WHEN
        Map<String, Object> result = CollectionUtils.copyMap(null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCopyMap_EmptyMap() {
        // GIVEN
        Map<String, Object> emptyMap = new HashMap<>();

        // WHEN
        Map<String, Object> result = CollectionUtils.copyMap(emptyMap);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCopyMap_NonEmptyMap() {
        // GIVEN
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("key1", "value1");
        userMap.put("key2", 2);

        // WHEN
        Map<String, Object> result = CollectionUtils.copyMap(userMap);

        // THEN
        assertNotNull(result);
        assertEquals(userMap.size(), result.size());
        assertEquals(userMap, result);
    }
}
