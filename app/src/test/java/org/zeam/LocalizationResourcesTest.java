package org.zeam;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalizationResourcesTest {
    private static final Pattern FORMAT_ARGUMENT = Pattern.compile("%(\\d+)\\$([a-zA-Z])");

    @Test
    public void portugueseStringsMatchTranslatableBaseStrings() throws Exception {
        Map<String, Element> baseStrings = translatableStrings(parse("values/strings.xml"));
        Map<String, Element> portugueseStrings = elements(parse("values-pt-rBR/strings.xml"), "string");

        assertEquals(baseStrings.keySet(), portugueseStrings.keySet());
    }

    @Test
    public void portugueseStringsPreserveFormatArguments() throws Exception {
        Map<String, Element> baseStrings = translatableStrings(parse("values/strings.xml"));
        Map<String, Element> portugueseStrings = elements(parse("values-pt-rBR/strings.xml"), "string");

        for (String name : baseStrings.keySet()) {
            assertEquals(name, formatArguments(baseStrings.get(name)), formatArguments(portugueseStrings.get(name)));
        }
    }

    @Test
    public void portugueseDisplayArraysPreserveNamesAndCounts() throws Exception {
        Map<String, Element> baseArrays = displayArrays(parse("values/arrays.xml"));
        Map<String, Element> portugueseArrays = elements(parse("values-pt-rBR/arrays.xml"), "array");

        assertEquals(baseArrays.keySet(), portugueseArrays.keySet());
        for (String name : baseArrays.keySet()) {
            assertEquals(name, itemCount(baseArrays.get(name)), itemCount(portugueseArrays.get(name)));
        }
    }

    @Test
    public void portugueseResourcesDoNotOverrideStableMetadata() throws Exception {
        Map<String, Element> portugueseStrings = elements(parse("values-pt-rBR/strings.xml"), "string");
        Map<String, Element> portugueseArrays = elements(parse("values-pt-rBR/arrays.xml"), "array");

        for (String name : portugueseStrings.keySet()) {
            assertFalse(name, name.startsWith("preferences_key_") || name.startsWith("preferences_default_"));
        }
        for (String name : portugueseArrays.keySet()) {
            assertTrue(name, name.startsWith("preferences_entries_"));
        }
    }

    private static Document parse(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(resourceFile(relativePath));
    }

    private static File resourceFile(String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleResource = new File(workingDirectory, "src/main/res/" + relativePath);
        if (moduleResource.isFile()) {
            return moduleResource;
        }
        return new File(workingDirectory, "app/src/main/res/" + relativePath);
    }

    private static Map<String, Element> translatableStrings(Document document) {
        Map<String, Element> strings = elements(document, "string");
        Map<String, Element> translatableStrings = new LinkedHashMap<String, Element>();
        for (Map.Entry<String, Element> entry : strings.entrySet()) {
            if (!"false".equals(entry.getValue().getAttribute("translatable"))) {
                translatableStrings.put(entry.getKey(), entry.getValue());
            }
        }
        return translatableStrings;
    }

    private static Map<String, Element> displayArrays(Document document) {
        Map<String, Element> arrays = elements(document, "array");
        Map<String, Element> displayArrays = new LinkedHashMap<String, Element>();
        for (Map.Entry<String, Element> entry : arrays.entrySet()) {
            if (entry.getKey().startsWith("preferences_entries_")) {
                displayArrays.put(entry.getKey(), entry.getValue());
            }
        }
        return displayArrays;
    }

    private static Map<String, Element> elements(Document document, String tagName) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(tagName);
        Map<String, Element> result = new LinkedHashMap<String, Element>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            result.put(element.getAttribute("name"), element);
        }
        return result;
    }

    private static List<String> formatArguments(Element element) {
        Matcher matcher = FORMAT_ARGUMENT.matcher(element.getTextContent());
        List<String> arguments = new ArrayList<String>();
        while (matcher.find()) {
            arguments.add(matcher.group(1) + "$" + matcher.group(2));
        }
        return arguments;
    }

    private static int itemCount(Element array) {
        int count = 0;
        NodeList children = array.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && "item".equals(child.getNodeName())) {
                count++;
            }
        }
        return count;
    }
}
