package org.zmreborn;

import org.junit.Test;
import org.xmlpull.v1.XmlSerializer;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.IOException;
import static org.junit.Assert.*;

public class FastXmlSerializerTest {

    @Test
    public void testBasicSerialization() throws IOException {
        FastXmlSerializer serializer = new FastXmlSerializer();
        StringWriter writer = new StringWriter();
        serializer.setOutput(writer);
        serializer.startDocument("utf-8", true);
        serializer.startTag(null, "root");
        serializer.attribute(null, "attr", "val");
        serializer.text("content");
        serializer.endTag(null, "root");
        serializer.endDocument();
        serializer.flush();

        String xml = writer.toString();
        assertTrue(xml.contains("<root attr=\"val\">content</root>"));
    }

    @Test
    public void testEscaping() throws IOException {
        FastXmlSerializer serializer = new FastXmlSerializer();
        StringWriter writer = new StringWriter();
        serializer.setOutput(writer);
        serializer.startDocument("utf-8", true);
        serializer.startTag(null, "escaped");
        serializer.attribute(null, "special", "a\"b&c<d>e");
        serializer.text("x\"y&z<w>v");
        serializer.endTag(null, "escaped");
        serializer.endDocument();
        serializer.flush();

        String xml = writer.toString();
        assertTrue(xml.contains("special=\"a&quot;b&amp;c&lt;d&gt;e\""));
        assertTrue(xml.contains("x&quot;y&amp;z&lt;w&gt;v"));
    }

    @Test
    public void testUnsupportedOperations() throws IOException {
        FastXmlSerializer serializer = new FastXmlSerializer();
        StringWriter writer = new StringWriter();
        serializer.setOutput(writer);

        try {
            serializer.getDepth();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            serializer.cdsect("test");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
