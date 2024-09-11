package com.eqh.application.utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GovtIdTCodeUtil {

    private static final Map<Integer, String> idTCodeMap = new HashMap<>();

    static {
        try {
            // Load XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = GovtIdTCodeUtil.class.getClassLoader().getResourceAsStream("OLI_LU_GOVTIDTC.xml");
            if (inputStream == null) {
                throw new IllegalStateException("XML file not found in classpath");
            }
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            // Parse XML
            NodeList allowedValueNodes = document.getElementsByTagName("fsc:allowedvalue");
            for (int i = 0; i < allowedValueNodes.getLength(); i++) {
                Element allowedValueElement = (Element) allowedValueNodes.item(i);

                // Extract and validate internal value
                String internalValueStr = allowedValueElement.getElementsByTagName("fsc:internal").item(0).getAttributes().getNamedItem("value").getNodeValue().trim();
                int internalValue = 0;
                try {
                    if (!internalValueStr.isEmpty()) {
                        internalValue = Integer.parseInt(internalValueStr);
                    }
                } catch (NumberFormatException e) {
                    // Log and skip invalid entries
                    System.err.println("Invalid number format in XML internal value: " + internalValueStr);
                    continue;
                }

                // Extract display name
                String displayName = "";
                NodeList displayNodes = allowedValueElement.getElementsByTagName("fsc:display");
                if (displayNodes.getLength() > 0) {
                    Element displayElement = (Element) displayNodes.item(0);
                    displayName = displayElement.getAttributes().getNamedItem("value").getNodeValue().trim();
                }

                idTCodeMap.put(internalValue, displayName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load or parse XML file", e);
        }
    }

    public static String getIdTCodeName(int code) {
        return idTCodeMap.getOrDefault(code, "Unknown");
    }
}
