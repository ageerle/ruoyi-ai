package org.ruoyi.common.core.utils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串工具类单元测试
 */
@Tag("dev")
@Tag("local")
class StringUtilsTest {

    @Test
    void testBlankToDefault() {
        assertEquals("default", StringUtils.blankToDefault("", "default"));
        assertEquals("default", StringUtils.blankToDefault("  ", "default"));
        assertEquals("default", StringUtils.blankToDefault(null, "default"));
        assertEquals("value", StringUtils.blankToDefault("value", "default"));
    }

    @Test
    void testIsEmptyAndIsNotEmpty() {
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty(null));
        assertFalse(StringUtils.isEmpty("test"));

        assertFalse(StringUtils.isNotEmpty(""));
        assertFalse(StringUtils.isNotEmpty(null));
        assertTrue(StringUtils.isNotEmpty("test"));
    }

    @Test
    void testFormat() {
        assertEquals("this is a for b", StringUtils.format("this is {} for {}", "a", "b"));
        assertEquals("this is {} for a", StringUtils.format("this is \\{} for {}", "a", "b"));
        assertEquals("this is \\a for b", StringUtils.format("this is \\\\{} for {}", "a", "b"));
    }

    @Test
    void testPadl() {
        // String left padding
        assertEquals("001", StringUtils.padl("1", 3, '0'));
        assertEquals("123", StringUtils.padl("123", 3, '0'));
        assertEquals("234", StringUtils.padl("1234", 3, '0'));
        
        // Number padding
        assertEquals("005", StringUtils.padl(5, 3));
    }

    @Test
    void testStr2List() {
        String str = "a,b,, c ,d";
        List<String> list1 = StringUtils.str2List(str, ",", false, false);
        assertEquals(5, list1.size());
        assertEquals(" c ", list1.get(3));

        List<String> list2 = StringUtils.str2List(str, ",", true, true);
        assertEquals(4, list2.size());
        assertEquals("c", list2.get(2));
    }

    @Test
    void testSplitList() {
        List<String> list = StringUtils.splitList("1,2,3");
        assertEquals(3, list.size());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertEquals("3", list.get(2));
    }
}
