package com.eqh.application.service;

import com.eqh.application.repository.TransactionHistoryRepository;
import com.eqh.application.utility.ResidenceStateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionHistoryService {

    @Autowired
    private TransactionHistoryRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    public void generateReport() throws IOException {
        List<Object[]> data = repository.findCustomTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        createReportsDirectoryIfNotExists();

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFilePath = "reports/transaction_history_report_" + timestamp + ".xlsx";

        List<List<Object>> transformedData = transformData(data);

        generateExcelReport(transformedData, excelFilePath);
    }

    private List<List<Object>> transformData(List<Object[]> data) {
        List<List<Object>> result = new ArrayList<>();

        for (Object[] row : data) {
            String jsonString = (String) row[0];
            BigDecimal grossAmt = (BigDecimal) row[1];
            Date transEffDate = (Date) row[2]; // Adjust if necessary

            try {
                JsonNode jsonMap = objectMapper.readTree(jsonString);

                String polNumber = Optional.ofNullable(jsonMap.get("polNumber")).map(JsonNode::asText).orElse("");
                JsonNode arrangement = jsonMap.get("arrangement");

                String productCode = "";
                if (arrangement != null) {
                    JsonNode arrSource = arrangement.get("arrSource");
                    if (arrSource != null && arrSource.isArray() && arrSource.size() > 0) {
                        JsonNode firstElement = arrSource.get(0);
                        productCode = Optional.ofNullable(firstElement.get("productCode")).map(JsonNode::asText).orElse("");
                    }
                }

                // Extract details from arrDestination
                JsonNode arrDestination = arrangement != null ? arrangement.get("arrDestination") : null;
                if (arrDestination != null && arrDestination.isArray()) {
                    for (JsonNode dest : arrDestination) {
                        JsonNode payee = dest.get("payee");
                        if (payee != null) {
                            JsonNode person = payee.get("person");
                            String firstName = person != null ? Optional.ofNullable(person.get("firstName")).map(JsonNode::asText).orElse("") : "";
                            String lastName = person != null ? Optional.ofNullable(person.get("lastName")).map(JsonNode::asText).orElse("") : "";
                            String residenceState = Optional.ofNullable(payee.get("residenceState")).map(JsonNode::asText).orElse("");

                            // Convert numeric residence state to HTML display value
                            int residenceStateCode = Integer.parseInt(residenceState);
                            String residenceStateText = ResidenceStateUtil.getStateName(residenceStateCode);

                            // Create a row for each payee
                            List<Object> transformedRow = new ArrayList<>();
                            transformedRow.add(polNumber);
                            transformedRow.add(productCode);
                            transformedRow.add(firstName); // Add firstName
                            transformedRow.add(lastName);  // Add lastName
                            transformedRow.add(residenceStateText); // Add the HTML display value
                            transformedRow.add(grossAmt != null ? grossAmt.toString() : "");
                            transformedRow.add(transEffDate != null ? new SimpleDateFormat("yyyy-MM-dd").format(transEffDate) : "");

                            result.add(transformedRow);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                List<Object> errorRow = Arrays.asList("", "", "", "", "", "", "");
                result.add(errorRow);
            }
        }

        return result;
    }

    private void generateExcelReport(List<List<Object>> data, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transaction History");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Policy Number",
                    "Product Code",
                    "First Name",
                    "Last Name",
                    "Residence State",
                    "Gross Amount",
                    "Transaction Effective Date"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Populate data rows
            int rowNum = 1;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(i);
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof BigDecimal) {
                        cell.setCellValue(((BigDecimal) value).doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void createReportsDirectoryIfNotExists() {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
    }
}
