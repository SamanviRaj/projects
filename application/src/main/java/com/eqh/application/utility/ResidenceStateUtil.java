package com.eqh.application.utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ResidenceStateUtil {

    private static final Map<Integer, String> stateMap = new HashMap<>();

    static {
        try {
            // Load XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = ResidenceStateUtil.class.getClassLoader().getResourceAsStream("OLI_LU_STATE.xml");
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
                    // Log and skip invalid entries
                    System.err.println("Invalid number format in XML code: " + codeStr);
                    continue;
                }

                // Extract display name
                String displayName = "";
                NodeList displayNodes = allowedValueElement.getElementsByTagName("fsc:display");
                if (displayNodes.getLength() > 0) {
                    Element displayElement = (Element) displayNodes.item(0);
                    displayName = displayElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                }

                stateMap.put(code, displayName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load or parse XML file", e);
        }
    }

    public static String getStateName(int code) {
        return stateMap.getOrDefault(code, "Unknown");
    }
}
