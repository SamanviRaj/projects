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
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";  // For parsing
    private static final String DATE_FORMAT_FOR_DISPLAY = "dd/MM/yyyy"; // For display
    private static final String CURRENCY_FORMAT = "$#,##0.00";

    private final PeriodicPayoutTransactionHistoryRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PeriodicPayoutTransactionHistoryService(PeriodicPayoutTransactionHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = repository.findCustomPayoutDeathClaimTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        List<List<Object>> transformedData = data.stream()
                .map(this::processRow)
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    private List<Object> processRow(Object[] row) {
        String messageImageJson = (String) row[0];
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(messageImageJson);
        } catch (IOException e) {
            logger.error("Error parsing JSON", e);
            return Arrays.asList("Error processing JSON");
        }

        Date transRunDate = parseDate(jsonNode.path("transRunDate").asText());
        String runYear = transRunDate == null ? "" : new SimpleDateFormat("yyyy").format(transRunDate);
        String suspendCode = jsonNode.path("suspendCode").asText();
        BigDecimal federalNonTaxableAmt = jsonNode.path("payeePayouts").get(0).path("federalNonTaxableAmt").decimalValue();
        BigDecimal grossAmt = jsonNode.path("payeePayouts").get(0).path("grossAmt").decimalValue();
        String endDate = jsonNode.path("benefits").get(0).path("endDate").asText();
        BigDecimal modalBenefit = jsonNode.path("benefits").get(0).path("modalBenefit").decimalValue();

        return Arrays.asList(
                runYear,
                formatDate(transRunDate),
                suspendCode,
                formatBigDecimal(federalNonTaxableAmt),
                formatBigDecimal(grossAmt),
                endDate,
                formatBigDecimal(modalBenefit)
        );
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL).parse(dateStr);
        } catch (Exception e) {
            logger.error("Error parsing date string: " + dateStr, e);
            return null;
        }
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) return "";
        DecimalFormat df = new DecimalFormat(CURRENCY_FORMAT);
        return df.format(value);
    }

    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT_FOR_DISPLAY).format(date);
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
        String[] headers = {
                "runYear",
                "transRunDate",
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
}
