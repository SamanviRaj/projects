package com.eqh.application.service;

import com.eqh.application.repository.PeriodicPayoutTransactionHistoryRepository;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PeriodicPayoutTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String CURRENCY_FORMAT = "$#,##0.00";

    private final PeriodicPayoutTransactionHistoryRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PeriodicPayoutTransactionHistoryService(PeriodicPayoutTransactionHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a report in Excel format and returns it as a byte array.
     *
     * @return byte array containing the Excel report.
     * @throws IOException if an error occurs during data retrieval or report generation.
     */
    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = repository.findCustomPayoutDeathClaimTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Transform the raw data into a format suitable for Excel
        List<List<Object>> transformedData = data.stream()
                .map(this::processRow)
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        // Generate the Excel report
        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    /**
     * Processes a single row of transaction data.
     *
     * @param row the raw data row.
     * @return a list of lists containing transformed data for each row.
     */
    private List<Object> processRow(Object[] row) {
        String messageImageJson = (String) row[0]; // Assuming message_image is at index 0
        BigDecimal grossAmount = (BigDecimal) row[1]; // Assuming gross_amt is at index 1
        Date transEffDate = (Date) row[2]; // Assuming trans_eff_date is at index 2
        Date transRunDate = (Date) row[3]; // Assuming trans_run_date is at index 3

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(messageImageJson);
        } catch (IOException e) {
            logger.error("Error parsing JSON", e);
            return Arrays.asList("Error processing JSON");
        }

        // Extract required fields from JSON
        String suspendCode = jsonNode.path("suspendCode").asText();
        BigDecimal federalNonTaxableAmt = jsonNode.path("payeePayouts").get(0).path("federalNonTaxableAmt").decimalValue();
        BigDecimal grossAmt = jsonNode.path("payeePayouts").get(0).path("grossAmt").decimalValue();
        String endDate = jsonNode.path("benefits").get(0).path("endDate").asText();
        BigDecimal modalBenefit = jsonNode.path("benefits").get(0).path("modalBenefit").decimalValue();

        // Return as List<Object>
        return Arrays.asList(
                suspendCode,
                formatBigDecimal(federalNonTaxableAmt),
                formatBigDecimal(grossAmt),
                endDate,
                formatBigDecimal(modalBenefit)
        );
    }

    /**
     * Formats a BigDecimal value as a currency string.
     *
     * @param value the BigDecimal value to format.
     * @return formatted currency string.
     */
    private String formatBigDecimal(BigDecimal value) {
        if (value == null) return "";
        DecimalFormat df = new DecimalFormat(CURRENCY_FORMAT);
        return df.format(value);
    }

    /**
     * Formats a Date object as a string in the format "yyyy-MM-dd".
     *
     * @param date the Date object to format.
     * @return formatted date string.
     */
    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL).format(date);
    }

    /**
     * Generates an Excel report from the given data and timestamp.
     *
     * @param data the data to populate the Excel sheet.
     * @param timestamp the timestamp to be included in the report file name.
     * @return byte array containing the Excel report.
     * @throws IOException if an error occurs during Excel report generation.
     */
    private byte[] generateExcelReportAsBytes(List<List<Object>> data, String timestamp) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transaction History");

            // Create the header row for the Excel sheet
            createHeaderRow(sheet);

            int rowNum = 1;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                // Populate each row with data
                populateRow(row, rowData, workbook);
            }

            // Auto-size columns to fit the content
            autoSizeColumns(sheet);

            // Write the workbook to a byte array output stream
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Creates the header row in the Excel sheet.
     *
     * @param sheet the sheet to which the header row will be added.
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Suspend Code",
                "Federal Non-Taxable Amount",
                "Gross Amount",
                "End Date",
                "Modal Benefit"
        };
        IntStream.range(0, headers.length).forEach(i -> {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        });
    }

    /**
     * Populates a row in the Excel sheet with data.
     *
     * @param row the row to populate.
     * @param rowData the data to be inserted into the row.
     * @param workbook the workbook used to create cell styles.
     */
    private void populateRow(Row row, List<Object> rowData, Workbook workbook) {
        IntStream.range(0, rowData.size()).forEach(i -> {
            Cell cell = row.createCell(i);
            Object value = rowData.get(i);
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
                CellStyle cellStyle = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                cellStyle.setDataFormat(format.getFormat(CURRENCY_FORMAT));
                cell.setCellStyle(cellStyle);
            } else {
                cell.setCellValue(value != null ? value.toString() : "");
            }
        });
    }

    /**
     * Auto-sizes all columns in the sheet to fit the content.
     *
     * @param sheet the sheet in which columns will be auto-sized.
     */
    private void autoSizeColumns(Sheet sheet) {
        int numberOfColumns = sheet.getRow(0).getLastCellNum();
        IntStream.range(0, numberOfColumns).forEach(sheet::autoSizeColumn);
    }
}
