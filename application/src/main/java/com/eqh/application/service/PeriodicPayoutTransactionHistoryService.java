package com.eqh.application.service;

import com.eqh.application.dto.Address;
import com.eqh.application.dto.ProductInfo;
import com.eqh.application.feignClient.PartyClient;
import com.eqh.application.repository.PeriodicPayoutTransactionHistoryRepository;
import com.eqh.application.repository.PolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class PeriodicPayoutTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String DATE_FORMAT_FOR_DISPLAY = "dd/MM/yyyy";
    private static final String CURRENCY_FORMAT = "$#,##0.00";
    private static final String[] HEADERS = {
            "runYear", "transRunDate", "Management Code", "Product Code", "polNumber", "Policy Status",
            "QualPlanType", "Suspend Code", "Party ID", "Party Full Name", "Govt ID", "Govt ID Status",
            "govt ID Type Code", "payeeStatus", "Residence State", "Residence Country", "preferredMailingAddress",
            "mailingAddress"
    };

    private final PeriodicPayoutTransactionHistoryRepository repository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat excelDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_DISPLAY);
    private final DecimalFormat currencyFormat = new DecimalFormat(CURRENCY_FORMAT);
    private final PartyClient partyClient;

    @Autowired
    public PeriodicPayoutTransactionHistoryService(
            PeriodicPayoutTransactionHistoryRepository repository,
            PolicyRepository policyRepository,
            ObjectMapper objectMapper,
            PartyClient partyClient) {
        this.repository = repository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
        this.partyClient = partyClient;
    }

    @Async
    public void generateReportInChunks(int pageSize) {
        new Thread(() -> {
            int offset = 0;
            List<Object[]> data;
            do {
                data = repository.findCustomPayoutDeathClaimTransactionsPaginated(pageSize, offset);
                if (!data.isEmpty()) {
                    try {
                        byte[] reportBytes = generateExcelReportAsBytes(processData(data), new SimpleDateFormat(DATE_FORMAT).format(new Date()));
                        saveReportToFile(reportBytes, "PeriodicPayoutReport_" + offset + ".xlsx");
                    } catch (IOException e) {
                        logger.error("Error generating report for offset " + offset, e);
                    }
                }
                offset += pageSize;
            } while (!data.isEmpty());
        }).start();
    }

    private List<List<Object>> processData(List<Object[]> data) {
        Set<String> policyNumbers = extractPolicyNumbers(data);
        Set<String> taxablePartyNumbers = extractTaxablePartyNumbers(data);
        Map<String, ProductInfo> productInfoMap = fetchProductInfoForPolicyNumbers(policyNumbers);
        Map<String, Map<String, String>> mailingAddressesMap = fetchMailingAddressesForTaxablePartyNumbers(taxablePartyNumbers);

        return data.stream()
                .map(row -> processRow(row, productInfoMap, mailingAddressesMap))
                .collect(Collectors.toList());
    }

    private Set<String> extractPolicyNumbers(List<Object[]> data) {
        return data.stream()
                .map(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[0]);
                        return jsonNode.path("polNumber").asText();
                    } catch (IOException e) {
                        logger.error("Error extracting policy number", e);
                        return "";
                    }
                })
                .filter(polNumber -> !polNumber.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> extractTaxablePartyNumbers(List<Object[]> data) {
        return data.stream()
                .flatMap(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[0]);
                        JsonNode payeePayoutsNode = jsonNode.path("payeePayouts");
                        return payeePayoutsNode.isArray()
                                ? StreamSupport.stream(payeePayoutsNode.spliterator(), false)
                                .map(payoutNode -> payoutNode.path("taxablePartyNumber").asText())
                                : Stream.empty();
                    } catch (IOException e) {
                        logger.error("Error extracting taxable party number", e);
                        return Stream.empty();
                    }
                })
                .filter(taxablePartyNumber -> !taxablePartyNumber.isEmpty())
                .collect(Collectors.toSet());
    }

    private void saveReportToFile(byte[] reportBytes, String filename) {
        try {
            // Get the user's home directory and append "Desktop" to it
            String desktopPath = System.getProperty("user.home") + "/Desktop";
            Path path = Path.of(desktopPath, filename);

            // Create the file and write the report bytes
            Files.write(path, reportBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            logger.info("Report saved to file: {}", path.toString());
        } catch (IOException e) {
            logger.error("Failed to save report to file: " + filename, e);
        }
    }

    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = repository.findCustomPayoutDeathClaimTransactions();
        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        Set<String> policyNumbers = extractPolicyNumbers(data);
        Map<String, ProductInfo> productInfoMap = fetchProductInfoForPolicyNumbers(policyNumbers);
        Set<String> taxablePartyNumbers = extractTaxablePartyNumbers(data);
        Map<String, Map<String, String>> mailingAddressesMap = fetchMailingAddressesForTaxablePartyNumbers(taxablePartyNumbers);

        List<List<Object>> transformedData = data.stream()
                .map(row -> processRow(row, productInfoMap, mailingAddressesMap))
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    private Map<String, ProductInfo> fetchProductInfoForPolicyNumbers(Set<String> policyNumbers) {
        List<String> policyNumberList = new ArrayList<>(policyNumbers);
        List<Object[]> productInfoList = policyRepository.findProductInfoByPolicyNumbers(policyNumberList);

        return productInfoList.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> new ProductInfo((String) arr[1], (String) arr[2], (String) arr[3], (String) arr[4])
                ));
    }

    private List<Object> processRow(Object[] row, Map<String, ProductInfo> productInfoMap, Map<String, Map<String, String>> mailingAddressesMap) {
        JsonNode jsonNode = parseJsonNode(row);
        String polNumber = jsonNode.path("polNumber").asText();
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("", "", "", ""));
        Date transRunDate = parseDate(jsonNode.path("transRunDate").asText());
        String runYear = Optional.ofNullable(transRunDate)
                .map(date -> new SimpleDateFormat("yyyy").format(date))
                .orElse("");

        // Extract values
        String suspendCode = jsonNode.path("suspendCode").asText();
        String taxablePartyNumber = extractTaxablePartyNumber(jsonNode);
        String preferredMailingAddress = getMailingAddress(mailingAddressesMap, taxablePartyNumber, "preferredAddress");
        String mailingAddress = getMailingAddress(mailingAddressesMap, taxablePartyNumber, "mailingAddress");

        // Prepare return values
        return Arrays.asList(
                runYear,
                formatDate(transRunDate),
                productInfo.getManagementCode(),
                productInfo.getProductCode(),
                polNumber,
                productInfo.getPolicyStatus(),
                "", // QualPlanType placeholder
                suspendCode,
                taxablePartyNumber,
                "", // Party Full Name placeholder
                "", // Govt ID placeholder
                "", // Govt ID Status placeholder
                "", // govt ID Type Code placeholder
                "", // payeeStatus placeholder
                "", // Residence State placeholder
                "", // Residence Country placeholder
                preferredMailingAddress,
                mailingAddress
        );
    }

    private JsonNode parseJsonNode(Object[] row) {
        try {
            return objectMapper.readTree((String) row[0]);
        } catch (IOException e) {
            logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
            return null;
        }
    }

    private String extractTaxablePartyNumber(JsonNode jsonNode) {
        // Logic to extract taxable party number from JSON
        return ""; // Placeholder
    }

    private String getMailingAddress(Map<String, Map<String, String>> mailingAddressesMap, String taxablePartyNumber, String addressType) {
        return Optional.ofNullable(mailingAddressesMap.get(taxablePartyNumber))
                .map(addresses -> addresses.getOrDefault(addressType, ""))
                .orElse("");
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return excelDateFormat.parse(dateStr);
        } catch (ParseException e) {
            logger.error("Error parsing date string: " + dateStr, e);
            return null;
        }
    }

    private String formatDate(Date date) {
        return date == null ? "" : displayDateFormat.format(date);
    }

    private byte[] generateExcelReportAsBytes(List<List<Object>> data, String timestamp) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transaction History");
            createHeaderRow(sheet);

            CellStyle currencyCellStyle = createCurrencyCellStyle(workbook);
            int rowNum = 1;

            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                populateRow(row, rowData, currencyCellStyle);
            }

            autoSizeColumns(sheet);
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private CellStyle createCurrencyCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat(CURRENCY_FORMAT));
        return cellStyle;
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        IntStream.range(0, HEADERS.length).forEach(i -> {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
        });
    }

    private void populateRow(Row row, List<Object> rowData, CellStyle currencyCellStyle) {
        IntStream.range(0, rowData.size()).forEach(i -> {
            Cell cell = row.createCell(i);
            Object value = rowData.get(i);
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
                cell.setCellStyle(currencyCellStyle);
            } else {
                cell.setCellValue(value != null ? value.toString() : "");
            }
        });
    }

    private void autoSizeColumns(Sheet sheet) {
        int numberOfColumns = sheet.getRow(0).getLastCellNum();
        IntStream.range(0, numberOfColumns).forEach(sheet::autoSizeColumn);
    }

    public Map<String, Map<String, String>> fetchMailingAddressesForTaxablePartyNumbers(Set<String> uniqueTaxablePartyNumbers) {
        // Initialize a map to hold the mailing addresses for each taxable party number
        Map<String, Map<String, String>> mailingAddressesMap = new HashMap<>();

        for (String taxablePartyNumber : uniqueTaxablePartyNumbers) {
            // Fetch addresses using Feign client
            List<Address> addresses = Collections.emptyList();
            if (taxablePartyNumber != null) {
                addresses = partyClient.getAddresses(taxablePartyNumber);
            }

            // Process addresses to get preferred and non-preferred mailing addresses
            String preferredMailingAddress = getFormattedAddress(
                    addresses.stream().filter(Address::getPrefAddr).findFirst().orElse(null)
            );

            String mailingAddress = getFormattedAddress(
                    addresses.stream().filter(address -> !address.getPrefAddr()).findFirst().orElse(null)
            );

            // Store the addresses in the map
            Map<String, String> addressesMap = new HashMap<>();
            addressesMap.put("preferredAddress", preferredMailingAddress);
            addressesMap.put("mailingAddress", mailingAddress);

            mailingAddressesMap.put(taxablePartyNumber, addressesMap);
        }

        return mailingAddressesMap;
    }

    private String getFormattedAddress(Address address) {
        if (address == null) {
            return "";
        }

        return Stream.of(
                        formatLine("Line 1", address.getLine1()),
                        formatLine("Line 2", address.getLine2()),
                        formatLine("Line 3", address.getLine3()),
                        formatLine("Line 4", address.getLine4()),
                        formatLine("Line 5", address.getLine5()),
                        formatLine("Zip", address.getZip())
                ).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String formatLine(String label, String value) {
        return (value != null) ? label + ": " + value + " " : "";
    }

    public byte[] getMessageImagesAsJson() throws IOException {
        List<Object[]> transactions = repository.findCustomPayoutDeathClaimTransactions();

        // Map to hold message images
        Map<String, JsonNode> messageImages = transactions.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // Assuming the first column is a string identifier
                        row -> {
                            Object jsonObject = row[1]; // This might be a String or BigDecimal
                            try {
                                if (jsonObject instanceof String) {
                                    return objectMapper.readTree((String) jsonObject);
                                } else if (jsonObject instanceof BigDecimal) {
                                    return objectMapper.readTree(((BigDecimal) jsonObject).toString());
                                } else {
                                    throw new IllegalArgumentException("Unexpected data type for JSON parsing: " + jsonObject.getClass());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Error parsing message_image JSON", e);
                            }
                        }
                ));


        // Convert the map to JSON
        return objectMapper.writeValueAsBytes(messageImages);
    }
}
