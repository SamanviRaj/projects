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

public class QualPlanUtil {

    private static final Logger logger = Logger.getLogger(QualPlanUtil.class.getName());
    private static final Map<Integer, String> displayNameMap = new HashMap<>();

    static {
        try (InputStream inputStream = QualPlanUtil.class.getClassLoader().getResourceAsStream("OLI_LU_QUALPLAN.xml")) {
            if (inputStream == null) {
                throw new IllegalStateException("XML file not found in classpath");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Ensure namespaces are handled correctly
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList allowedValueNodes = document.getElementsByTagName("fsc:allowedvalue");
            for (int i = 0; i < allowedValueNodes.getLength(); i++) {
                Element allowedValueElement = (Element) allowedValueNodes.item(i);

                // Extract internal value
                String internalValueStr = allowedValueElement.getElementsByTagName("fsc:internal").item(0)
                        .getAttributes().getNamedItem("value").getNodeValue().trim();
                int internalValue;
                try {
                    internalValue = internalValueStr.isEmpty() ? 0 : Integer.parseInt(internalValueStr);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid number format in XML internal value: " + internalValueStr, e);
                    continue;
                }

                // Extract display name
                String displayName = "";
                NodeList externalNodes = allowedValueElement.getElementsByTagName("fsc:external");
                if (externalNodes.getLength() > 0) {
                    Element externalElement = (Element) externalNodes.item(0);
                    NodeList displayNodes = externalElement.getElementsByTagName("fsc:display");
                    if (displayNodes.getLength() > 0) {
                        Element displayElement = (Element) displayNodes.item(0);
                        displayName = displayElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                    }
                }

                displayNameMap.put(internalValue, displayName);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse XML file", e);
            throw new RuntimeException("Failed to load or parse XML file", e);
        }
    }

    public static String getDisplayName(int code) {
        return displayNameMap.getOrDefault(code, "Unknown");
    }
}

