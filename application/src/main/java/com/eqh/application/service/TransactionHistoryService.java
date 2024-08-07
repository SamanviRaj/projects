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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss"; // Desired date format
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String CURRENCY_FORMAT = "$#,##0.00";

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = transactionHistoryRepository.findCustomTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        List<List<Object>> transformedData = transformData(data);

        // Create timestamp
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    private List<List<Object>> transformData(List<Object[]> data) {
        List<List<Object>> result = new ArrayList<>();

        for (Object[] row : data) {
            try {
                JsonNode jsonMap = objectMapper.readTree((String) row[0]);
                String polNumber = Optional.ofNullable(jsonMap.get("polNumber")).map(JsonNode::asText).orElse("");
                String productCode = Optional.ofNullable(policyRepository.findProductCodeByPolicyNumber(polNumber)).orElse("Unknown");

                JsonNode arrangement = jsonMap.get("arrangement");
                JsonNode arrDestination = arrangement != null ? arrangement.get("arrDestination") : null;

                if (arrDestination != null && arrDestination.isArray()) {
                    for (JsonNode dest : arrDestination) {
                        processDestination(result, dest, (BigDecimal) row[1], (Date) row[2], polNumber, productCode);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing row", e);
                result.add(Arrays.asList("", "", "", "", "", "", "", "", "Error processing row"));
            }
        }

        return result;
    }

    private void processDestination(List<List<Object>> result, JsonNode dest, BigDecimal grossAmt, Date transEffDate, String polNumber, String productCode) {
        JsonNode payee = dest.get("payee");
        if (payee != null) {
            JsonNode person = payee.get("person");
            String firstName = Optional.ofNullable(person.get("firstName")).map(JsonNode::asText).orElse("");
            String lastName = Optional.ofNullable(person.get("lastName")).map(JsonNode::asText).orElse("");
            String residenceState = Optional.ofNullable(payee.get("residenceState")).map(JsonNode::asText).orElse("");
            int residenceStateCode = Integer.parseInt(residenceState);
            String residenceStateText = ResidenceStateUtil.getStateName(residenceStateCode);

            BigDecimal settlementInterestAmt = Optional.ofNullable(dest.get("settlementInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
            BigDecimal lateInterestAmt = Optional.ofNullable(dest.get("lateInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);

            List<Object> transformedRow = Arrays.asList(
                    polNumber,
                    productCode,
                    firstName,
                    lastName,
                    residenceStateText,
                    grossAmt != null ? grossAmt.toString() : "",
                    transEffDate != null ? new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL).format(transEffDate) : "",
                    settlementInterestAmt != null ? settlementInterestAmt.toString() : "",
                    lateInterestAmt != null ? lateInterestAmt.toString() : ""
            );

            result.add(transformedRow);
        }
    }

    private byte[] generateExcelReportAsBytes(List<List<Object>> data, String timestamp) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transaction History");

            createHeaderRow(sheet);

            int rowNum = 1;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                populateRow(row, rowData, workbook);
            }

            autoSizeColumns(sheet);

            workbook.write(baos);
            baos.flush();
            return baos.toByteArray();
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Policy Number",
                "Product Code",
                "First Name",
                "Last Name",
                "Residence State",
                "Gross Amount",
                "Transaction Effective Date",
                "Settlement Interest Amount",
                "Late Interest Amount"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private void populateRow(Row row, List<Object> rowData, Workbook workbook) {
        for (int i = 0; i < rowData.size(); i++) {
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
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        int numberOfColumns = sheet.getRow(0).getLastCellNum();
        for (int i = 0; i < numberOfColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
