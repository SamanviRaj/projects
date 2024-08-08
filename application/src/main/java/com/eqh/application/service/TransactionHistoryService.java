package com.eqh.application.service;

import com.eqh.application.repository.PolicyRepository;
import com.eqh.application.repository.TransactionHistoryRepository;
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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for generating transaction history reports.
 */
@Service
public class TransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String CURRENCY_FORMAT = "$#,##0.00";
    private static final String ERROR_PROCESSING_ROW = "Error processing row";

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Generates the transaction history report as a byte array.
     *
     * @return Byte array representing the Excel report.
     * @throws IOException If an error occurs while generating the report.
     */
    public byte[] generateReportAsBytes() throws IOException {
        // Fetch transaction data from the repository
        List<Object[]> data = transactionHistoryRepository.findCustomTransactions();

        // Check if there is no data
        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Transform data into the required format
        List<List<Object>> transformedData = transformData(data);

        // Create a timestamp for the report
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        // Generate and return the Excel report as byte array
        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    /**
     * Transforms the raw data into a structured format suitable for the report.
     *
     * @param data Raw transaction data.
     * @return Transformed data as a list of rows.
     */
    private List<List<Object>> transformData(List<Object[]> data) {
        List<List<Object>> result = new ArrayList<>();

        // Iterate through each row of data
        for (Object[] row : data) {
            try {
                // Parse the JSON string
                JsonNode jsonMap = objectMapper.readTree((String) row[0]);

                // Extract policy number and product code
                String polNumber = Optional.ofNullable(jsonMap.get("polNumber")).map(JsonNode::asText).orElse("");
                String productCode = Optional.ofNullable(policyRepository.findProductCodeByPolicyNumber(polNumber)).orElse("Unknown");

                // Process each destination in the arrangement
                JsonNode arrangement = jsonMap.get("arrangement");
                JsonNode arrDestination = arrangement != null ? arrangement.get("arrDestination") : null;

                if (arrDestination != null && arrDestination.isArray()) {
                    for (JsonNode dest : arrDestination) {
                        processDestination(result, dest, (BigDecimal) row[1], (Date) row[2], polNumber, productCode);
                    }
                }
            } catch (Exception e) {
                // Log error and add an error row to the result
                logger.error(ERROR_PROCESSING_ROW, e);
                result.add(Collections.singletonList(ERROR_PROCESSING_ROW));
            }
        }

        return result;
    }

    /**
     * Processes a destination entry and adds it to the result list.
     *
     * @param result The list where processed rows are added.
     * @param dest JSON node representing the destination.
     * @param grossAmt Gross amount of the transaction.
     * @param transEffDate Effective date of the transaction.
     * @param polNumber Policy number.
     * @param productCode Product code.
     */
    private void processDestination(List<List<Object>> result, JsonNode dest, BigDecimal grossAmt, Date transEffDate, String polNumber, String productCode) {
        JsonNode payee = dest.get("payee");
        if (payee != null) {
            // Extract payee details
            JsonNode person = payee.get("person");
            String firstName = Optional.ofNullable(person.get("firstName")).map(JsonNode::asText).orElse("");
            String lastName = Optional.ofNullable(person.get("lastName")).map(JsonNode::asText).orElse("");
            String residenceState = Optional.ofNullable(payee.get("residenceState")).map(JsonNode::asText).orElse("");
            int residenceStateCode = Integer.parseInt(residenceState);
            String residenceStateText = ResidenceStateUtil.getStateName(residenceStateCode);

            // Extract financial details
            BigDecimal settlementInterestAmt = Optional.ofNullable(dest.get("settlementInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
            BigDecimal lateInterestAmt = Optional.ofNullable(dest.get("lateInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
            BigDecimal deathBenefitPayoutAmt = Optional.ofNullable(dest.get("deathBenefitPayoutAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
            String partyNumber = Optional.ofNullable(payee.get("partyNumber")).map(JsonNode::asText).orElse("");

            // Create a row for the report
            List<Object> transformedRow = Arrays.asList(
                    productCode,          // Product code
                    polNumber,            // Policy number
                    partyNumber,          // Party number
                    firstName,            // First name
                    lastName,             // Last name
                    residenceStateText,   // Residence state
                    formatDate(transEffDate),  // Transaction effective date
                    formatBigDecimal(settlementInterestAmt), // Settlement interest amount
                    formatBigDecimal(lateInterestAmt),      // Late interest amount
                    formatBigDecimal(deathBenefitPayoutAmt)  // Death benefit payout amount
            );

            result.add(transformedRow);
        }
    }

    /**
     * Formats BigDecimal values into strings for Excel reporting.
     *
     * @param value BigDecimal value to format.
     * @return Formatted string.
     */
    private String formatBigDecimal(BigDecimal value) {
        return value != null ? value.toString() : "";
    }

    /**
     * Formats Date values into strings suitable for Excel.
     *
     * @param date Date value to format.
     * @return Formatted date string.
     */
    private String formatDate(Date date) {
        return date != null ? new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL).format(date) : "";
    }

    /**
     * Generates an Excel report from the transformed data.
     *
     * @param data Transformed report data.
     * @param timestamp Timestamp for the report.
     * @return Byte array representing the Excel report.
     * @throws IOException If an error occurs while creating the report.
     */
    private byte[] generateExcelReportAsBytes(List<List<Object>> data, String timestamp) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Create an Excel sheet
            Sheet sheet = workbook.createSheet("Transaction History");

            // Create header row
            createHeaderRow(sheet);

            // Populate data rows
            int rowNum = 1;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                populateRow(row, rowData, workbook);
            }

            // Auto-size columns for better readability
            autoSizeColumns(sheet);

            // Write the workbook to the output stream
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Creates the header row in the Excel sheet.
     *
     * @param sheet The Excel sheet to add headers to.
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Product Code",
                "Policy Number",
                "Party Number",
                "First Name",
                "Last Name",
                "Residence State",
                "Transaction Effective Date",
                "Settlement Interest Amount",
                "Late Interest Amount",
                "Gross Amount"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    /**
     * Populates a row in the Excel sheet with data.
     *
     * @param row The row to populate.
     * @param rowData Data to be placed in the row.
     * @param workbook The Excel workbook (for cell styles).
     */
    private void populateRow(Row row, List<Object> rowData, Workbook workbook) {
        for (int i = 0; i < rowData.size(); i++) {
            Cell cell = row.createCell(i);
            Object value = rowData.get(i);
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
                // Apply currency format to BigDecimal cells
                CellStyle cellStyle = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                cellStyle.setDataFormat(format.getFormat(CURRENCY_FORMAT));
                cell.setCellStyle(cellStyle);
            } else {
                cell.setCellValue(value != null ? value.toString() : "");
            }
        }
    }

    /**
     * Auto-sizes columns to fit the content.
     *
     * @param sheet The Excel sheet to adjust column sizes.
     */
    private void autoSizeColumns(Sheet sheet) {
        int numberOfColumns = sheet.getRow(0).getLastCellNum();
        for (int i = 0; i < numberOfColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
