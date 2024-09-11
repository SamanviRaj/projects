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

public class PolicyStatusUtil {

    private static final Logger logger = Logger.getLogger(PolicyStatusUtil.class.getName());
    private static final Map<Integer, String> policyStatusMap = new HashMap<>();

    static {
        try (InputStream inputStream = PolicyStatusUtil.class.getClassLoader().getResourceAsStream("OLI_LU_POLSTAT.xml")) {
            if (inputStream == null) {
                throw new IllegalStateException("XML file not found in classpath");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList allowedValueNodes = document.getElementsByTagName("fsc:allowedvalue");
            for (int i = 0; i < allowedValueNodes.getLength(); i++) {
                Element allowedValueElement = (Element) allowedValueNodes.item(i);

                // Extract and validate internal value
                String internalValueStr = "";
                NodeList internalNodes = allowedValueElement.getElementsByTagName("fsc:internal");
                if (internalNodes.getLength() > 0) {
                    Element internalElement = (Element) internalNodes.item(0);
                    internalValueStr = internalElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                }

                int internalValue = 0;
                try {
                    if (!internalValueStr.isEmpty()) {
                        internalValue = Integer.parseInt(internalValueStr);
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid number format in XML internal value: " + internalValueStr, e);
                    continue;
                }

                // Extract display name
                String displayName = "";
                NodeList displayNodes = allowedValueElement.getElementsByTagName("fsc:display");
                if (displayNodes.getLength() > 0) {
                    Element displayElement = (Element) displayNodes.item(0);
                    displayName = displayElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                }

                policyStatusMap.put(internalValue, displayName);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse XML file", e);
            throw new RuntimeException("Failed to load or parse XML file", e);
        }
    }

    public static String getPolicyStatusName(int code) {
        return policyStatusMap.getOrDefault(code, "Unknown");
    }
}
