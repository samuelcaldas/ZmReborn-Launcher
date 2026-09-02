package org.zmreborn;

import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Utilities for serializing and deserializing data structures to and from XML.
 */
public final class XmlUtils {

    private XmlUtils() {
    }

    /**
     * Skips the current XML tag and all of its descendants.
     */
    public static void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser == null) {
            throw new IllegalArgumentException("XmlPullParser must not be null");
        }
        int outerDepth = parser.getDepth();
        int eventType;
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) {
                return;
            }
        }
    }

    /**
     * Finds the index of a value within an array of string options, or returns the default value.
     */
    public static int convertValueToList(CharSequence value, String[] options, int defaultValue) {
        if (value == null || options == null) {
            return defaultValue;
        }
        for (int index = 0; index < options.length; index++) {
            if (value.equals(options[index])) {
                return index;
            }
        }
        return defaultValue;
    }

    /**
     * Converts a string representation to a boolean value.
     */
    public static boolean convertValueToBoolean(CharSequence value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    /**
     * Parses an integer from a string supporting decimal, hex, and octal notations.
     */
    public static int convertValueToInt(CharSequence charSequence, int defaultValue) {
        if (charSequence == null || charSequence.length() == 0) {
            return defaultValue;
        }
        String text = charSequence.toString();
        int sign = 1;
        int index = 0;
        int length = text.length();
        int radix = 10;
        if (text.charAt(0) == '-') {
            sign = -1;
            index = 1;
        }
        if (index >= length) {
            return defaultValue;
        }
        if (text.charAt(index) == '0') {
            if (index == length - 1) {
                return 0;
            }
            char nextChar = text.charAt(index + 1);
            if (nextChar == 'x' || nextChar == 'X') {
                index += 2;
                radix = 16;
            } else {
                index++;
                radix = 8;
            }
        } else if (text.charAt(index) == '#') {
            index++;
            radix = 16;
        }
        return Integer.parseInt(text.substring(index), radix) * sign;
    }

    /**
     * Parses an unsigned integer attribute with fallback to a default value.
     */
    public static int convertValueToUnsignedInt(String value, int defaultValue) {
        return value == null ? defaultValue : parseUnsignedIntAttribute(value);
    }

    /**
     * Parses an unsigned integer string into an int value.
     */
    public static int parseUnsignedIntAttribute(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            throw new IllegalArgumentException("Input string must not be empty");
        }
        String text = charSequence.toString();
        int index = 0;
        int length = text.length();
        int radix = 10;
        if (text.charAt(0) == '0') {
            if (length == 1) {
                return 0;
            }
            char nextChar = text.charAt(1);
            if (nextChar == 'x' || nextChar == 'X') {
                index = 2;
                radix = 16;
            } else {
                index = 1;
                radix = 8;
            }
        } else if (text.charAt(0) == '#') {
            index = 1;
            radix = 16;
        }
        return (int) Long.parseLong(text.substring(index), radix);
    }

    /**
     * Serializes a map to the given output stream using fast XML formatting.
     */
    public static void writeMapXml(Map<String, ?> map, OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlSerializer serializer = new FastXmlSerializer();
        serializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeMapXml(map, null, serializer);
        serializer.endDocument();
    }

    /**
     * Serializes a list to the given output stream in XML format.
     */
    public static void writeListXml(List<?> list, OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeListXml(list, null, serializer);
        serializer.endDocument();
    }

    /**
     * Serializes a map as a nested element within an XML document.
     */
    public static void writeMapXml(Map<?, ?> map, String name, XmlSerializer serializer) throws XmlPullParserException, IOException {
        if (map == null) {
            serializer.startTag(null, "null");
            serializer.endTag(null, "null");
            return;
        }
        serializer.startTag(null, "map");
        if (name != null) {
            serializer.attribute(null, "name", name);
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeValueXml(entry.getValue(), entry.getKey() != null ? entry.getKey().toString() : null, serializer);
        }
        serializer.endTag(null, "map");
    }

    /**
     * Serializes a list as a nested element within an XML document.
     */
    public static void writeListXml(List<?> list, String name, XmlSerializer serializer) throws XmlPullParserException, IOException {
        if (list == null) {
            serializer.startTag(null, "null");
            serializer.endTag(null, "null");
            return;
        }
        serializer.startTag(null, "list");
        if (name != null) {
            serializer.attribute(null, "name", name);
        }
        int count = list.size();
        for (int index = 0; index < count; index++) {
            writeValueXml(list.get(index), null, serializer);
        }
        serializer.endTag(null, "list");
    }

    /**
     * Serializes a byte array as a hex string inside an XML element.
     */
    public static void writeByteArrayXml(byte[] bytes, String name, XmlSerializer serializer) throws XmlPullParserException, IOException {
        if (bytes == null) {
            serializer.startTag(null, "null");
            serializer.endTag(null, "null");
            return;
        }
        serializer.startTag(null, "byte-array");
        if (name != null) {
            serializer.attribute(null, "name", name);
        }
        serializer.attribute(null, "num", Integer.toString(bytes.length));
        StringBuilder hexBuilder = new StringBuilder(bytes.length * 2);
        for (byte byteValue : bytes) {
            int unsignedValue = byteValue & 255;
            hexBuilder.append(Character.forDigit((unsignedValue >>> 4) & 15, 16));
            hexBuilder.append(Character.forDigit(unsignedValue & 15, 16));
        }
        serializer.text(hexBuilder.toString());
        serializer.endTag(null, "byte-array");
    }

    /**
     * Serializes an int array as a collection of child items in an XML element.
     */
    public static void writeIntArrayXml(int[] integers, String name, XmlSerializer serializer) throws XmlPullParserException, IOException {
        if (integers == null) {
            serializer.startTag(null, "null");
            serializer.endTag(null, "null");
            return;
        }
        serializer.startTag(null, "int-array");
        if (name != null) {
            serializer.attribute(null, "name", name);
        }
        serializer.attribute(null, "num", Integer.toString(integers.length));
        for (int number : integers) {
            serializer.startTag(null, "item");
            serializer.attribute(null, "value", Integer.toString(number));
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, "int-array");
    }

    /**
     * Serializes any supported value object to XML.
     */
    public static void writeValueXml(Object value, String name, XmlSerializer serializer) throws XmlPullParserException, IOException {
        if (value == null) {
            serializer.startTag(null, "null");
            if (name != null) {
                serializer.attribute(null, "name", name);
            }
            serializer.endTag(null, "null");
            return;
        }
        if (value instanceof String) {
            serializer.startTag(null, "string");
            if (name != null) {
                serializer.attribute(null, "name", name);
            }
            serializer.text(value.toString());
            serializer.endTag(null, "string");
            return;
        }
        if (value instanceof byte[]) {
            writeByteArrayXml((byte[]) value, name, serializer);
            return;
        }
        if (value instanceof int[]) {
            writeIntArrayXml((int[]) value, name, serializer);
            return;
        }
        if (value instanceof Map) {
            writeMapXml((Map<?, ?>) value, name, serializer);
            return;
        }
        if (value instanceof List) {
            writeListXml((List<?>) value, name, serializer);
            return;
        }
        if (value instanceof CharSequence) {
            serializer.startTag(null, "string");
            if (name != null) {
                serializer.attribute(null, "name", name);
            }
            serializer.text(value.toString());
            serializer.endTag(null, "string");
            return;
        }
        String typeTag = resolveScalarTypeTag(value);
        serializer.startTag(null, typeTag);
        if (name != null) {
            serializer.attribute(null, "name", name);
        }
        serializer.attribute(null, "value", value.toString());
        serializer.endTag(null, typeTag);
    }

    private static String resolveScalarTypeTag(Object value) {
        if (value instanceof Integer) {
            return "int";
        }
        if (value instanceof Long) {
            return "long";
        }
        if (value instanceof Float) {
            return "float";
        }
        if (value instanceof Double) {
            return "double";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        throw new IllegalArgumentException("writeValueXml: unable to write value " + value);
    }

    /**
     * Deserializes an XML document into a HashMap.
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> readMapXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);
        return (HashMap<String, Object>) readValueXml(parser, new String[1]);
    }

    /**
     * Deserializes an XML document into an ArrayList.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Object> readListXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);
        return (ArrayList<Object>) readValueXml(parser, new String[1]);
    }

    /**
     * Deserializes map entries until reaching the specified matching end tag.
     */
    public static HashMap<String, Object> readThisMapXml(XmlPullParser parser, String endTag, String[] nameHolder) throws XmlPullParserException, IOException {
        HashMap<String, Object> map = new HashMap<>();
        int eventType = parser.getEventType();
        do {
            if (eventType == XmlPullParser.START_TAG) {
                Object value = readThisValueXml(parser, nameHolder);
                if (nameHolder[0] != null) {
                    map.put(nameHolder[0], value);
                } else {
                    throw new XmlPullParserException("Map value without name attribute: " + parser.getName());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(endTag)) {
                    return map;
                }
                throw new XmlPullParserException("Expected " + endTag + " end tag at: " + parser.getName());
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        throw new XmlPullParserException("Document ended before " + endTag + " end tag");
    }

    /**
     * Deserializes list items until reaching the specified matching end tag.
     */
    public static ArrayList<Object> readThisListXml(XmlPullParser parser, String endTag, String[] nameHolder) throws XmlPullParserException, IOException {
        ArrayList<Object> list = new ArrayList<>();
        int eventType = parser.getEventType();
        do {
            if (eventType == XmlPullParser.START_TAG) {
                list.add(readThisValueXml(parser, nameHolder));
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(endTag)) {
                    return list;
                }
                throw new XmlPullParserException("Expected " + endTag + " end tag at: " + parser.getName());
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        throw new XmlPullParserException("Document ended before " + endTag + " end tag");
    }

    /**
     * Deserializes an int array until reaching the specified matching end tag.
     */
    public static int[] readThisIntArrayXml(XmlPullParser parser, String endTag, String[] nameHolder) throws XmlPullParserException, IOException {
        String numAttribute = parser.getAttributeValue(null, "num");
        if (numAttribute == null) {
            throw new XmlPullParserException("Need num attribute in int-array");
        }
        int size;
        try {
            size = Integer.parseInt(numAttribute);
        } catch (NumberFormatException exception) {
            throw new XmlPullParserException("Not a number in num attribute in int-array");
        }
        int[] array = new int[size];
        int index = 0;
        int eventType = parser.getEventType();
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (!"item".equals(parser.getName())) {
                    throw new XmlPullParserException("Expected item tag at: " + parser.getName());
                }
                String valueAttr = parser.getAttributeValue(null, "value");
                if (valueAttr == null) {
                    throw new XmlPullParserException("Need value attribute in item");
                }
                try {
                    array[index] = Integer.parseInt(valueAttr);
                } catch (NumberFormatException exception) {
                    throw new XmlPullParserException("Not a number in value attribute in item");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(endTag)) {
                    return array;
                }
                if ("item".equals(parser.getName())) {
                    index++;
                } else {
                    throw new XmlPullParserException("Expected " + endTag + " end tag at: " + parser.getName());
                }
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        throw new XmlPullParserException("Document ended before " + endTag + " end tag");
    }

    /**
     * Reads a value element from the XML pull parser.
     */
    public static Object readValueXml(XmlPullParser parser, String[] nameHolder) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.START_TAG) {
            if (eventType == XmlPullParser.END_TAG) {
                throw new XmlPullParserException("Unexpected end tag at: " + parser.getName());
            }
            if (eventType == XmlPullParser.TEXT) {
                throw new XmlPullParserException("Unexpected text: " + parser.getText());
            }
            eventType = parser.next();
            if (eventType == XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException("Unexpected end of document");
            }
        }
        return readThisValueXml(parser, nameHolder);
    }

    private static Object readThisValueXml(XmlPullParser parser, String[] nameHolder) throws XmlPullParserException, IOException {
        String valueName = parser.getAttributeValue(null, "name");
        String tagName = parser.getName();
        if ("null".equals(tagName)) {
            consumeEndTag(parser, "null");
            nameHolder[0] = valueName;
            return null;
        }
        if ("string".equals(tagName)) {
            String stringValue = readStringContent(parser);
            nameHolder[0] = valueName;
            return stringValue;
        }
        if ("int".equals(tagName)) {
            Object intValue = Integer.valueOf(parser.getAttributeValue(null, "value"));
            consumeEndTag(parser, tagName);
            nameHolder[0] = valueName;
            return intValue;
        }
        if ("long".equals(tagName)) {
            Object longValue = Long.valueOf(parser.getAttributeValue(null, "value"));
            consumeEndTag(parser, tagName);
            nameHolder[0] = valueName;
            return longValue;
        }
        if ("float".equals(tagName)) {
            Object floatValue = Float.valueOf(parser.getAttributeValue(null, "value"));
            consumeEndTag(parser, tagName);
            nameHolder[0] = valueName;
            return floatValue;
        }
        if ("double".equals(tagName)) {
            Object doubleValue = Double.valueOf(parser.getAttributeValue(null, "value"));
            consumeEndTag(parser, tagName);
            nameHolder[0] = valueName;
            return doubleValue;
        }
        if ("boolean".equals(tagName)) {
            Object booleanValue = Boolean.valueOf(parser.getAttributeValue(null, "value"));
            consumeEndTag(parser, tagName);
            nameHolder[0] = valueName;
            return booleanValue;
        }
        if ("int-array".equals(tagName)) {
            parser.next();
            int[] arrayValue = readThisIntArrayXml(parser, "int-array", nameHolder);
            nameHolder[0] = valueName;
            return arrayValue;
        }
        if ("map".equals(tagName)) {
            parser.next();
            HashMap<String, Object> mapValue = readThisMapXml(parser, "map", nameHolder);
            nameHolder[0] = valueName;
            return mapValue;
        }
        if ("list".equals(tagName)) {
            parser.next();
            ArrayList<Object> listValue = readThisListXml(parser, "list", nameHolder);
            nameHolder[0] = valueName;
            return listValue;
        }
        throw new XmlPullParserException("Unknown tag: " + tagName);
    }

    private static String readStringContent(XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder result = new StringBuilder();
        int eventType;
        while (true) {
            eventType = parser.next();
            if (eventType == XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException("Unexpected end of document in <string>");
            }
            if (eventType == XmlPullParser.END_TAG) {
                if ("string".equals(parser.getName())) {
                    return result.toString();
                }
                throw new XmlPullParserException("Unexpected end tag in <string>: " + parser.getName());
            }
            if (eventType == XmlPullParser.TEXT) {
                result.append(parser.getText());
            } else if (eventType == XmlPullParser.START_TAG) {
                throw new XmlPullParserException("Unexpected start tag in <string>: " + parser.getName());
            }
        }
    }

    private static void consumeEndTag(XmlPullParser parser, String expectedTagName) throws XmlPullParserException, IOException {
        int eventType;
        do {
            eventType = parser.next();
            if (eventType == XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException("Unexpected end of document in <" + expectedTagName + ">");
            }
            if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(expectedTagName)) {
                    return;
                }
                throw new XmlPullParserException("Unexpected end tag in <" + expectedTagName + ">: " + parser.getName());
            }
            if (eventType == XmlPullParser.START_TAG) {
                throw new XmlPullParserException("Unexpected start tag in <" + expectedTagName + ">: " + parser.getName());
            }
        } while (eventType != XmlPullParser.START_TAG);
    }

    /**
     * Positions the parser at the start tag of the first element with the given name.
     */
    public static void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            // Skip non-start elements
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() + ", expected " + firstElementName);
        }
    }

    /**
     * Advances the parser to the next start tag or document end.
     */
    public static void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            // Skip non-start elements
        }
    }
}
