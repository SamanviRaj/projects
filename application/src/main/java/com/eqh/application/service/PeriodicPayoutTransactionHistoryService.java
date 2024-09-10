package com.eqh.application.service;

import com.eqh.application.repository.PeriodicPayoutTransactionHistoryRepository;
import com.eqh.application.repository.PolicyRepository;
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
            "Management Code", "Policy Status", "Product Code"
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
        String messageImageJson = (String) row[0];
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(messageImageJson);
        } catch (IOException e) {
            logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
            return Arrays.asList("Error processing JSON");
        }

        String polNumber = jsonNode.path("polNumber").asText();
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("", "", ""));

        Date transRunDate = parseDate(jsonNode.path("transRunDate").asText());
        String runYear = transRunDate == null ? "" : new SimpleDateFormat("yyyy").format(transRunDate);
        String suspendCode = jsonNode.path("suspendCode").asText();
        BigDecimal federalNonTaxableAmt = jsonNode.path("payeePayouts").get(0).path("federalNonTaxableAmt").decimalValue();
        BigDecimal grossAmt = jsonNode.path("payeePayouts").get(0).path("grossAmt").decimalValue();
        String endDate = jsonNode.path("benefits").get(0).path("endDate").asText();
        BigDecimal modalBenefit = jsonNode.path("benefits").get(0).path("modalBenefit").decimalValue();

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
                productInfo.getPolicyStatus(),
                productInfo.getProductCode()
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
