package org.zmreborn;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlSerializer;

/**
 * Fast, low-allocation XML serializer optimized for simple structured data persistence.
 */
public class FastXmlSerializer implements XmlSerializer {
    private static final int BUFFER_LENGTH = 8192;
    private static final String[] ESCAPE_TABLE;
    private boolean inTag;
    private int position;
    private final char[] buffer = new char[BUFFER_LENGTH];
    private Writer writer;

    static {
        String[] escapeTable = new String[64];
        escapeTable[34] = "&quot;";
        escapeTable[38] = "&amp;";
        escapeTable[60] = "&lt;";
        escapeTable[62] = "&gt;";
        ESCAPE_TABLE = escapeTable;
    }

    private void append(char character) throws IOException {
        int currentPosition = this.position;
        if (currentPosition >= BUFFER_LENGTH - 1) {
            flush();
            currentPosition = this.position;
        }
        this.buffer[currentPosition] = character;
        this.position = currentPosition + 1;
    }

    private void append(String text, int offset, int length) throws IOException {
        if (text == null || length <= 0) {
            return;
        }
        if (length > BUFFER_LENGTH) {
            int end = offset + length;
            int current = offset;
            while (current < end) {
                int next = current + BUFFER_LENGTH;
                append(text, current, next < end ? BUFFER_LENGTH : end - current);
                current = next;
            }
            return;
        }
        int currentPosition = this.position;
        if (currentPosition + length > BUFFER_LENGTH) {
            flush();
            currentPosition = this.position;
        }
        text.getChars(offset, offset + length, this.buffer, currentPosition);
        this.position = currentPosition + length;
    }

    private void append(char[] characterBuffer, int offset, int length) throws IOException {
        if (characterBuffer == null || length <= 0) {
            return;
        }
        if (length > BUFFER_LENGTH) {
            int end = offset + length;
            int current = offset;
            while (current < end) {
                int next = current + BUFFER_LENGTH;
                append(characterBuffer, current, next < end ? BUFFER_LENGTH : end - current);
                current = next;
            }
            return;
        }
        int currentPosition = this.position;
        if (currentPosition + length > BUFFER_LENGTH) {
            flush();
            currentPosition = this.position;
        }
        System.arraycopy(characterBuffer, offset, this.buffer, currentPosition, length);
        this.position = currentPosition + length;
    }

    private void append(String text) throws IOException {
        if (text != null) {
            append(text, 0, text.length());
        }
    }

    private void escapeAndAppendString(String string) throws IOException {
        if (string == null) {
            return;
        }
        int stringLength = string.length();
        char escapeTableLength = (char) ESCAPE_TABLE.length;
        String[] escapes = ESCAPE_TABLE;
        int lastPosition = 0;
        int currentPosition = 0;
        while (currentPosition < stringLength) {
            char character = string.charAt(currentPosition);
            if (character < escapeTableLength) {
                String escape = escapes[character];
                if (escape != null) {
                    if (lastPosition < currentPosition) {
                        append(string, lastPosition, currentPosition - lastPosition);
                    }
                    lastPosition = currentPosition + 1;
                    append(escape);
                }
            }
            currentPosition++;
        }
        if (lastPosition < currentPosition) {
            append(string, lastPosition, currentPosition - lastPosition);
        }
    }

    private void escapeAndAppendString(char[] characterBuffer, int start, int length) throws IOException {
        if (characterBuffer == null || length <= 0) {
            return;
        }
        char escapeTableLength = (char) ESCAPE_TABLE.length;
        String[] escapes = ESCAPE_TABLE;
        int end = start + length;
        int lastPosition = start;
        int currentPosition = start;
        while (currentPosition < end) {
            char character = characterBuffer[currentPosition];
            if (character < escapeTableLength) {
                String escape = escapes[character];
                if (escape != null) {
                    if (lastPosition < currentPosition) {
                        append(characterBuffer, lastPosition, currentPosition - lastPosition);
                    }
                    lastPosition = currentPosition + 1;
                    append(escape);
                }
            }
            currentPosition++;
        }
        if (lastPosition < currentPosition) {
            append(characterBuffer, lastPosition, currentPosition - lastPosition);
        }
    }

    /**
     * Appends an attribute with optional namespace and escaped value.
     */
    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name must not be null");
        }
        append(' ');
        if (namespace != null) {
            append(namespace);
            append(':');
        }
        append(name);
        append("=\"");
        escapeAndAppendString(value != null ? value : "");
        append('\"');
        return this;
    }

    @Override
    public void cdsect(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void comment(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void docdecl(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endDocument() throws IOException {
        flush();
    }

    /**
     * Closes the currently open tag or emits a matching closing tag.
     */
    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException {
        if (this.inTag) {
            append(" />\n");
            this.inTag = false;
            return this;
        }
        append("</");
        if (namespace != null) {
            append(namespace);
            append(':');
        }
        append(name);
        append(">\n");
        this.inTag = false;
        return this;
    }

    @Override
    public void entityRef(String text) {
        throw new UnsupportedOperationException();
    }

    /**
     * Flushes buffered characters to the underlying writer.
     */
    @Override
    public void flush() throws IOException {
        if (this.position == 0) {
            return;
        }
        if (this.writer == null) {
            throw new IllegalStateException("Writer output has not been set");
        }
        this.writer.write(this.buffer, 0, this.position);
        this.writer.flush();
        this.position = 0;
    }

    @Override
    public int getDepth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getFeature(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ignorableWhitespace(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processingInstruction(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFeature(String name, boolean state) {
        if (!"http://xmlpull.org/v1/doc/features.html#indent-output".equals(name)) {
            throw new UnsupportedOperationException("Unsupported feature: " + name);
        }
    }

    /**
     * Sets the destination output stream and character encoding.
     */
    @Override
    public void setOutput(OutputStream outputStream, String encoding) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream must not be null");
        }
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        this.writer = new OutputStreamWriter(outputStream, charset);
    }

    /**
     * Sets the destination writer directly.
     */
    @Override
    public void setOutput(Writer destinationWriter) {
        if (destinationWriter == null) {
            throw new IllegalArgumentException("Writer must not be null");
        }
        this.writer = destinationWriter;
    }

    @Override
    public void setPrefix(String prefix, String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes the standard XML declaration tag.
     */
    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException {
        boolean isStandalone = standalone != null && standalone;
        append("<?xml version='1.0' encoding='utf-8' standalone='" + (isStandalone ? "yes" : "no") + "' ?>\n");
    }

    /**
     * Opens a new XML element tag.
     */
    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Tag name must not be null");
        }
        if (this.inTag) {
            append(">\n");
        }
        append('<');
        if (namespace != null) {
            append(namespace);
            append(':');
        }
        append(name);
        this.inTag = true;
        return this;
    }

    /**
     * Writes text from a character buffer, escaping XML special characters.
     */
    @Override
    public XmlSerializer text(char[] characterBuffer, int start, int length) throws IOException {
        if (this.inTag) {
            append(">");
            this.inTag = false;
        }
        escapeAndAppendString(characterBuffer, start, length);
        return this;
    }

    /**
     * Writes text string, escaping XML special characters.
     */
    @Override
    public XmlSerializer text(String textContent) throws IOException {
        if (this.inTag) {
            append(">");
            this.inTag = false;
        }
        escapeAndAppendString(textContent);
        return this;
    }
}
