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

public class ResidenceCountryUtil {

    private static final Logger logger = Logger.getLogger(ResidenceCountryUtil.class.getName());
    private static final Map<Integer, String> countryMap = new HashMap<>();

    static {
        try {
            // Load XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = ResidenceCountryUtil.class.getClassLoader().getResourceAsStream("OLI_LU_NATION.xml");
            if (inputStream == null) {
                throw new IllegalStateException("XML file not found in classpath");
            }
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            // Parse XML
            NodeList allowedValueNodes = document.getElementsByTagName("fsc:allowedvalue");
            for (int i = 0; i < allowedValueNodes.getLength(); i++) {
                Element allowedValueElement = (Element) allowedValueNodes.item(i);

                // Extract and validate code
                String codeStr = allowedValueElement.getElementsByTagName("fsc:internal").item(0).getAttributes().getNamedItem("value").getNodeValue().trim();
                int code = 0;
                try {
                    if (!codeStr.isEmpty()) {
                        code = Integer.parseInt(codeStr);
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid number format in XML code: " + codeStr, e);
                    continue;
                }

                // Extract display name
                String displayName = "Unknown";  // Default value
                NodeList displayNodes = allowedValueElement.getElementsByTagName("fsc:display");
                for (int j = 0; j < displayNodes.getLength(); j++) {
                    Element displayElement = (Element) displayNodes.item(j);
                    String type = displayElement.getAttributes().getNamedItem("type").getNodeValue();
                    if ("html".equals(type)) {
                        displayName = displayElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                        break;  // Use the first html type display value
                    }
                }

                countryMap.put(code, displayName);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse XML file", e);
            throw new RuntimeException("Failed to load or parse XML file", e);
        }
    }

    public static String getCountryName(int code) {
        return countryMap.getOrDefault(code, "Unknown");
    }
}

