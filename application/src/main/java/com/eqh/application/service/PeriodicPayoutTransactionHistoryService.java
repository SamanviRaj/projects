package com.eqh.application.service;

import com.eqh.application.dto.Address;
import com.eqh.application.repository.PeriodicPayoutTransactionHistoryRepository;
import com.eqh.application.repository.PolicyRepository;
import com.eqh.application.utility.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import com.eqh.application.feignClient.PartyClient;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class PeriodicPayoutTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String DATE_FORMAT_FOR_DISPLAY = "dd/MM/yyyy";
    private static final String CURRENCY_FORMAT = "$#,##0.00";


    private static final String[] HEADERS = {
            "runYear", "transRunDate","Management Code",  "Product Code", "polNumber", "Policy Status",
            "QualPlanType","Suspend Code","Party ID", "Party Full Name","Govt ID", "Govt ID Status",
            "govt ID Type Code","payeeStatus", "Residence State" , "Residence Country", "preferredMailingAddress",
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

    public byte[] getMessageImagesAsJson() throws IOException {
        LocalDateTime startDate = LocalDateTime.of(2024, 7, 15, 0, 0);

        List<Object[]> transactions = repository.findCustomPayoutDeathClaimTransactions(startDate);

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

    public byte[] generateReportAsBytes() throws IOException {
        LocalDateTime startDate = LocalDateTime.of(2024, 7, 15, 0, 0);
        List<Object[]> data = repository.findCustomPayoutDeathClaimTransactions(startDate);

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Extract unique policy numbers
        Set<String> policyNumbers = data.stream()
                .map(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[0]);
                        return jsonNode.path("polNumber").asText();
                    } catch (IOException e) {
                        logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
                        return "";
                    }
                })
                .filter(polNumber -> !polNumber.isEmpty())
                .collect(Collectors.toSet());

        // Fetch product info for all policy numbers
        Map<String, ProductInfo> productInfoMap = fetchProductInfoForPolicyNumbers(policyNumbers);

        // Extract unique taxablePartyNumbers
        Set<String> uniqueTaxablePartyNumbers = data.stream()
                .map(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[0]);
                        JsonNode payeePayoutsNode = jsonNode.path("payeePayouts");
                        Set<String> taxablePartyNumbers = new HashSet<>();
                        if (payeePayoutsNode.isArray()) {
                            for (JsonNode payoutNode : payeePayoutsNode) {
                                String taxablePartyNumber = payoutNode.path("taxablePartyNumber").asText();
                                if (!taxablePartyNumber.isEmpty()) {
                                    taxablePartyNumbers.add(taxablePartyNumber);
                                }
                            }
                        }
                        return taxablePartyNumbers;
                    } catch (IOException e) {
                        logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
                        return new HashSet<String>();
                    }
                })
                .flatMap(Set::stream) // Flatten the set of sets
                .collect(Collectors.toSet()); // Collect unique taxablePartyNumbers

        // Print unique taxablePartyNumbers
        System.out.println("Unique Taxable Party Numbers:");
        uniqueTaxablePartyNumbers.forEach(System.out::println);

        Map<String, Map<String, String>> mailingAddressesMap = fetchMailingAddressesForTaxablePartyNumbers(uniqueTaxablePartyNumbers);

        // Transform data
        // Transform data
        List<List<Object>> transformedData = data.stream()
                .map(row -> processRow(row, productInfoMap, mailingAddressesMap))
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
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


    private Map<String, ProductInfo> fetchProductInfoForPolicyNumbers(Set<String> policyNumbers) {
        // Convert Set to List
        List<String> policyNumberList = new ArrayList<>(policyNumbers);

        // Fetch product info for all policy numbers
        List<Object[]> productInfoList = policyRepository.findProductInfoByPolicyNumbers(policyNumberList);

        // Map the results to a Map for quick lookup
        return productInfoList.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0], // polNumber
                        arr -> new ProductInfo((String) arr[1], (String) arr[2], (String) arr[3], (String) arr[4]) // managementCode, policyStatus, productCode
                ));
    }

    private List<Object> processRow(Object[] row, Map<String, ProductInfo> productInfoMap, Map<String, Map<String, String>> mailingAddressesMap) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree((String) row[0]);
        } catch (IOException e) {
            logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
            return Collections.singletonList("Error processing JSON");
        }

        String polNumber = jsonNode.path("polNumber").asText();
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("", "", "", ""));

        Date transRunDate = parseDate(jsonNode.path("transRunDate").asText());
        String runYear = Optional.ofNullable(transRunDate)
                .map(date -> new SimpleDateFormat("yyyy").format(date))
                .orElse("");
        String suspendCode = jsonNode.path("suspendCode").asText();

        // Initialize variables for aggregated or extracted data
        BigDecimal federalNonTaxableAmt = BigDecimal.ZERO;
        BigDecimal grossAmt = BigDecimal.ZERO;
        BigDecimal modalBenefit = BigDecimal.ZERO;
        String taxablePartyNumber = "";
        String endDate = "";
        String taxableToGovtID = "";
        String taxablePartyName = "";
        String taxableToGovtIDStatus = "";
        String taxableToGovtIdTCode = "";
        String taxableToResidenceState = "";
        String payeeStatus = "";
        String taxableToResidenceCountry = "";

        // Process payeePayouts array
        JsonNode payeePayoutsNode = jsonNode.path("payeePayouts");
        if (payeePayoutsNode.isArray()) {
            for (JsonNode payout : payeePayoutsNode) {
                federalNonTaxableAmt = federalNonTaxableAmt.add(payout.path("federalNonTaxableAmt").decimalValue());
                grossAmt = grossAmt.add(payout.path("grossAmt").decimalValue());
                taxablePartyNumber = payout.path("taxablePartyNumber").asText(taxablePartyNumber); // Use last non-empty value
                taxableToGovtID = payout.path("taxableToGovtID").asText(taxableToGovtID); // Use last non-empty value
                taxablePartyName = payout.path("taxablePartyName").asText(taxablePartyName); // Use last non-empty value
                taxableToGovtIDStatus = payout.path("taxableToGovtIDStat").asText(taxableToGovtIDStatus); // Use last non-empty value
                taxableToGovtIdTCode = payout.path("taxableToGovtIdTC").asText(taxableToGovtIdTCode); // Use last non-empty value
                taxableToResidenceState = payout.path("taxableToResidenceState").asText(taxableToResidenceState); // Use last non-empty value
                payeeStatus = payout.path("payeeStatus").asText(payeeStatus); // Use last non-empty value
                taxableToResidenceCountry = payout.path("taxableToResidenceCountry").asText(taxableToResidenceCountry);
            }
        }

        // Process benefits array
        JsonNode benefitsNode = jsonNode.path("benefits");
        if (benefitsNode.isArray()) {
            for (JsonNode benefit : benefitsNode) {
                endDate = benefit.path("endDate").asText(endDate); // Use last non-empty value
                modalBenefit = modalBenefit.add(benefit.path("modalBenefit").decimalValue()); // Aggregate modal benefits
            }
        }

        // Transform suspendCode using SuspendCodeUtil
        String transformedSuspendCode = SuspendCodeUtil.getSuspendCodeName(suspendCode);

        // Transform QualPlanType using QualPlanUtil
        String transformedQualPlanType = "Unknown";
        if (productInfo.getQualPlanType() != null && !productInfo.getQualPlanType().trim().isEmpty()) {
            try {
                String qualPlanType = productInfo.getQualPlanType().trim();
                transformedQualPlanType = QualPlanUtil.getDisplayName(Integer.parseInt(qualPlanType));
            } catch (Exception e) {
                logger.warn("Error transforming QualPlanType: " + productInfo.getQualPlanType(), e);
            }
        } else {
            logger.warn("Empty or null QualPlanType: " + productInfo.getQualPlanType());
        }

        // Transform taxableToResidenceState using ResidenceStateUtil
        String transformedResidenceState = "Unknown";
        if (taxableToResidenceState != null && !taxableToResidenceState.trim().isEmpty()) {
            try {
                int residenceStateCode = Integer.parseInt(taxableToResidenceState.trim());
                transformedResidenceState = ResidenceStateUtil.getStateName(residenceStateCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid residence state code: " + taxableToResidenceState, e);
            }
        } else {
            logger.warn("Empty or null residence state code: " + taxableToResidenceState);
        }


        // Transform taxableToResidenceCountry using ResidenceStateUtil
        String transformedResidenceCountry = "Unknown";
        if (taxableToResidenceCountry != null && !taxableToResidenceCountry.trim().isEmpty()) {
            try {
                int residenceCountryCode = Integer.parseInt(taxableToResidenceCountry.trim());
                transformedResidenceCountry = ResidenceCountryUtil.getCountryName(residenceCountryCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid residence state code: " + taxableToResidenceCountry, e);
            }
        } else {
            logger.warn("Empty or null residence state code: " + taxableToResidenceCountry);
        }

        // Transform taxableToGovtIDStatus using GovtIDStatusUtil
        String transformedGovtIDStatus = "Unknown";
        if (taxableToGovtIDStatus != null && !taxableToGovtIDStatus.trim().isEmpty()) {
            try {
                int govtIDStatusCode = Integer.parseInt(taxableToGovtIDStatus.trim());
                transformedGovtIDStatus = GovtIDStatusUtil.getStatusName(govtIDStatusCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid government ID status code: " + taxableToGovtIDStatus, e);
            }
        } else {
            logger.warn("Empty or null government ID status code: " + taxableToGovtIDStatus);
        }

        // Transform taxableToGovtIdTCode using GovtIdTCodeUtil
        String transformedGovtIdTCode = "Unknown";
        if (taxableToGovtIdTCode != null && !taxableToGovtIdTCode.trim().isEmpty()) {
            try {
                int govtIdTCode = Integer.parseInt(taxableToGovtIdTCode.trim());
                transformedGovtIdTCode = GovtIdTCodeUtil.getIdTCodeName(govtIdTCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid government ID type code: " + taxableToGovtIdTCode, e);
            }
        } else {
            logger.warn("Empty or null government ID type code: " + taxableToGovtIdTCode);
        }

        // Transform productInfo policy status using PolicyStatusUtil
        String transformedPolicyStatus = "Unknown";
        if (productInfo.getPolicyStatus() != null && !productInfo.getPolicyStatus().trim().isEmpty()) {
            try {
                int policyStatusCode = Integer.parseInt(productInfo.getPolicyStatus().trim());
                transformedPolicyStatus = PolicyStatusUtil.getPolicyStatusName(policyStatusCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid policy status code: " + productInfo.getPolicyStatus(), e);
            }
        } else {
            logger.warn("Empty or null policy status code: " + productInfo.getPolicyStatus());
        }

        // Transform payeeStatus using PayeeStatusUtil
        String transformedPayeeStatus = PayeeStatusUtil.getDisplayName(Integer.parseInt(payeeStatus.trim()));

        // Retrieve addresses from the map
        Map<String, String> addressesMap = mailingAddressesMap.getOrDefault(taxablePartyNumber, new HashMap<>());
        String preferredMailingAddress = addressesMap.getOrDefault("preferredAddress", "");
        String mailingAddress = addressesMap.getOrDefault("mailingAddress", "");

        return Arrays.asList(
                runYear,
                formatDate(transRunDate),
                productInfo.getManagementCode(),
                productInfo.getProductCode(),
                polNumber,
                transformedPolicyStatus, // Updated to use transformed value
                transformedQualPlanType, // Updated to use transformed value
                transformedSuspendCode, // Updated to use transformed value
                taxablePartyNumber,
                taxablePartyName,
                taxableToGovtID,
                transformedGovtIDStatus,
                transformedGovtIdTCode,
                transformedPayeeStatus, // Updated to use transformed value
                transformedResidenceState,
                transformedResidenceCountry,
                preferredMailingAddress,
                mailingAddress
        );
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

    private String formatBigDecimal(BigDecimal value) {
        return value == null ? "" : currencyFormat.format(value);
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

    // Helper class to store product information
    private static class ProductInfo {
        private final String managementCode;
        private final String policyStatus;
        private final String productCode;

        private final String qualPlanType;

        public ProductInfo(String managementCode, String policyStatus, String productCode,String qual_plan_type) {
            this.managementCode = managementCode;
            this.policyStatus = policyStatus;
            this.productCode = productCode;
            this.qualPlanType =  qual_plan_type;
        }

        public String getManagementCode() {
            return managementCode;
        }

        public String getPolicyStatus() {
            return policyStatus;
        }

        public String getProductCode() {
            return productCode;
        }
        public String getQualPlanType() {
            return qualPlanType;
        }
    }
}
