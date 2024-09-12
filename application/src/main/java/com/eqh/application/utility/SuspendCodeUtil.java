package com.eqh.application.utility;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SuspendCodeUtil {

    private static final Logger logger = LoggerFactory.getLogger(SuspendCodeUtil.class);
    private static final Map<String, String> suspendCodeMap = new HashMap<>();

    static {
        try (InputStream inputStream = SuspendCodeUtil.class.getResourceAsStream("/OLIEXT_LU_SUSPEND.xml")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("XML file not found");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            NodeList allowedValues = document.getElementsByTagName("fsc:allowedvalue");
            for (int i = 0; i < allowedValues.getLength(); i++) {
                Element element = (Element) allowedValues.item(i);
                String internalValue = element.getElementsByTagName("fsc:internal").item(0).getAttributes().getNamedItem("value").getTextContent();
                String displayValue = element.getElementsByTagName("fsc:display").item(0).getAttributes().getNamedItem("value").getTextContent();
                suspendCodeMap.put(internalValue, displayValue);
            }
        } catch (ParserConfigurationException | IOException | org.xml.sax.SAXException e) {
            logger.error("Error loading or parsing XML", e);
        }
    }

    public static String getSuspendCodeName(String internalCode) {
        return suspendCodeMap.getOrDefault(internalCode, "Unknown");
    }
}
