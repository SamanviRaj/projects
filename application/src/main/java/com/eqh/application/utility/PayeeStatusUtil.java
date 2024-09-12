package com.eqh.application.utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayeeStatusUtil {

    private static final Logger logger = Logger.getLogger(PayeeStatusUtil.class.getName());
    private static final Map<Integer, String> displayNameMap = new HashMap<>();
    private static final String XML_FILE = "OLIEXT_LU_PAYEESTATUS.xml";
    private static final String ALLOWED_VALUE_TAG = "fsc:allowedvalue";
    private static final String INTERNAL_TAG = "fsc:internal";
    private static final String EXTERNAL_TAG = "fsc:external";
    private static final String DISPLAY_TAG = "fsc:display";
    private static final String VALUE_ATTR = "value";
    private static final String ID_ATTR = "id";
    private static final String LOCALE_ATTR = "locale";

    static {
        try (InputStream inputStream = PayeeStatusUtil.class.getClassLoader().getResourceAsStream(XML_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("XML file not found in classpath: " + XML_FILE);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Set this if XML uses namespaces
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList allowedValueNodes = document.getElementsByTagName(ALLOWED_VALUE_TAG);
            for (int i = 0; i < allowedValueNodes.getLength(); i++) {
                Element allowedValueElement = (Element) allowedValueNodes.item(i);

                // Extract internal value
                String internalValueStr = allowedValueElement.getElementsByTagName(INTERNAL_TAG).item(0)
                        .getAttributes().getNamedItem(VALUE_ATTR).getNodeValue().trim();
                int internalValue;
                try {
                    internalValue = Integer.parseInt(internalValueStr);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid number format in XML internal value: " + internalValueStr, e);
                    continue;
                }

                // Extract display name
                String displayName = "";
                NodeList externalNodes = allowedValueElement.getElementsByTagName(EXTERNAL_TAG);
                if (externalNodes.getLength() > 0) {
                    Element externalElement = (Element) externalNodes.item(0);
                    NodeList displayNodes = externalElement.getElementsByTagName(DISPLAY_TAG);
                    if (displayNodes.getLength() > 0) {
                        Element displayElement = (Element) displayNodes.item(0);
                        displayName = displayElement.getAttributes().getNamedItem(VALUE_ATTR).getNodeValue().trim();
                    }
                }

                displayNameMap.put(internalValue, displayName);
            }

            // Optional: Handle keysets if needed
            // For now, we’re only concerned with the allowed values

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse XML file: " + XML_FILE, e);
            throw new RuntimeException("Failed to load or parse XML file: " + XML_FILE, e);
        }
    }

    public static String getDisplayName(int code) {
        return displayNameMap.getOrDefault(code, "Unknown");
    }
}
