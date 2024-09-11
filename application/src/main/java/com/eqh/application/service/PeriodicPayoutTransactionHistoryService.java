package com.eqh.application.service;

import com.eqh.application.repository.PeriodicPayoutTransactionHistoryRepository;
import com.eqh.application.repository.PolicyRepository;
import com.eqh.application.utility.GovtIDStatusUtil;
import com.eqh.application.utility.GovtIdTCodeUtil;
import com.eqh.application.utility.PolicyStatusUtil;
import com.eqh.application.utility.ResidenceStateUtil;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PeriodicPayoutTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String DATE_FORMAT_FOR_DISPLAY = "dd/MM/yyyy";
    private static final String CURRENCY_FORMAT = "$#,##0.00";

    private static final String[] HEADERS = {
            "runYear", "polNumber", "transRunDate", "Suspend Code",
            "Federal Non-Taxable Amount", "Gross Amount", "End Date", "Modal Benefit",
            "Management Code", "Product Code", "Policy Status", "Party ID", "Govt ID", "Party Full Name", "Govt ID Status",
            "govt ID Type Code", "Residence State", "payeeStatus"
    };

    private final PeriodicPayoutTransactionHistoryRepository repository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat excelDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_DISPLAY);
    private final DecimalFormat currencyFormat = new DecimalFormat(CURRENCY_FORMAT);

    @Autowired
    public PeriodicPayoutTransactionHistoryService(
            PeriodicPayoutTransactionHistoryRepository repository,
            PolicyRepository policyRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }

    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = repository.findCustomPayoutDeathClaimTransactions();

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

        // Transform data
        List<List<Object>> transformedData = data.stream()
                .map(row -> processRow(row, productInfoMap))
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
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
                        arr -> new ProductInfo((String) arr[1], (String) arr[2], (String) arr[3]) // managementCode, policyStatus, productCode
                ));
    }

    private List<Object> processRow(Object[] row, Map<String, ProductInfo> productInfoMap) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree((String) row[0]);
        } catch (IOException e) {
            logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
            return Collections.singletonList("Error processing JSON");
        }

        String polNumber = jsonNode.path("polNumber").asText();
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("", "", ""));

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

        return Arrays.asList(
                runYear,
                polNumber,
                formatDate(transRunDate),
                suspendCode,
                formatBigDecimal(federalNonTaxableAmt),
                formatBigDecimal(grossAmt),
                endDate,
                formatBigDecimal(modalBenefit),
                productInfo.getManagementCode(),
                productInfo.getProductCode(),
                transformedPolicyStatus, // Updated to use transformed value
                taxablePartyNumber,
                taxableToGovtID,
                taxablePartyName,
                transformedGovtIDStatus,
                transformedGovtIdTCode,
                transformedResidenceState,
                payeeStatus
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

        public ProductInfo(String managementCode, String policyStatus, String productCode) {
            this.managementCode = managementCode;
            this.policyStatus = policyStatus;
            this.productCode = productCode;
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
    }
}
