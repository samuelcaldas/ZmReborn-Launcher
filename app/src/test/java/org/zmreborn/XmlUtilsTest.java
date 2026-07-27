package org.zmreborn;

import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class XmlUtilsTest {

    @Test
    public void testConvertValueToBoolean() {
        assertTrue(XmlUtils.convertValueToBoolean("true", false));
        assertTrue(XmlUtils.convertValueToBoolean("TRUE", false));
        assertTrue(XmlUtils.convertValueToBoolean("1", false));
        assertFalse(XmlUtils.convertValueToBoolean("false", true));
        assertFalse(XmlUtils.convertValueToBoolean("0", true));
        assertTrue(XmlUtils.convertValueToBoolean(null, true));
        assertFalse(XmlUtils.convertValueToBoolean("invalid", false));
    }

    @Test
    public void testConvertValueToInt() {
        assertEquals(42, XmlUtils.convertValueToInt("42", 0));
        assertEquals(0, XmlUtils.convertValueToInt(null, 0));
        assertEquals(10, XmlUtils.convertValueToInt("0xa", 0));
        assertEquals(8, XmlUtils.convertValueToInt("010", 0)); // octal
    }

    @Test
    public void testConvertValueToList() {
        String[] options = new String[]{"a", "b", "c"};
        assertEquals(1, XmlUtils.convertValueToList("b", options, -1));
        assertEquals(-1, XmlUtils.convertValueToList(null, options, -1));
        assertEquals(-1, XmlUtils.convertValueToList("d", options, -1));
    }
}
