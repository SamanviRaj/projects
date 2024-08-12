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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
public class TransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String CURRENCY_FORMAT = "$#,##0.00";
    private static final String ERROR_PROCESSING_ROW = "Error processing row";
    private static final String UNKNOWN = "Unknown";

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionHistoryService(TransactionHistoryRepository transactionHistoryRepository,
                                     PolicyRepository policyRepository,
                                     ObjectMapper objectMapper) {
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }

    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = transactionHistoryRepository.findCustomTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Load all policy-product mappings in advance
        Map<String, String> policyProductCodes = loadPolicyProductCodes();

        List<List<Object>> transformedData = data.stream()
                .flatMap(row -> processRow(row, policyProductCodes).stream())
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    public byte[] getJsonDataAsBytes() throws IOException {
        List<Object[]> data = transactionHistoryRepository.findCustomTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the JSON data.");
        }

        List<JsonNode> jsonNodes = data.stream()
                .map(row -> {
                    try {
                        return objectMapper.readTree((String) row[0]);
                    } catch (IOException e) {
                        logger.error(ERROR_PROCESSING_ROW, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return objectMapper.writeValueAsBytes(jsonNodes);
    }

    private Map<String, String> loadPolicyProductCodes() {
        List<Object[]> policies = policyRepository.findAllPolicyNumbersWithProductCodes();
        return policies.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // Policy Number
                        row -> (String) row[1]  // Product Code
                ));
    }

    private List<List<Object>> processRow(Object[] row, Map<String, String> policyProductCodes) {
        List<List<Object>> result = new ArrayList<>();
        try {
            JsonNode jsonMap = objectMapper.readTree((String) row[0]);

            String polNumber = Optional.ofNullable(jsonMap.get("polNumber")).map(JsonNode::asText).orElse(UNKNOWN);
            String productCode = policyProductCodes.getOrDefault(polNumber, UNKNOWN); // Use pre-fetched data

            JsonNode arrDestination = Optional.ofNullable(jsonMap.get("arrangement"))
                    .map(arr -> arr.get("arrDestination"))
                    .orElse(null);

            if (arrDestination != null && arrDestination.isArray() && !productCode.equalsIgnoreCase(UNKNOWN)) {
                result.addAll(StreamSupport.stream(arrDestination.spliterator(), false)
                        .map(dest -> processDestination(dest, (BigDecimal) row[1], (Date) row[2], polNumber, productCode))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            logger.error(ERROR_PROCESSING_ROW, e);
            result.add(Collections.singletonList(ERROR_PROCESSING_ROW));
        }
        return result;
    }

    private List<List<Object>> processDestination(JsonNode dest, BigDecimal grossAmt, Date transEffDate, String polNumber, String productCode) {
        JsonNode payee = dest.get("payee");
        if (payee == null) return Collections.emptyList();

        String firstName = Optional.ofNullable(payee.get("person")).map(person -> person.get("firstName").asText()).orElse(UNKNOWN);
        String lastName = Optional.ofNullable(payee.get("person")).map(person -> person.get("lastName").asText()).orElse(UNKNOWN);
        String residenceState = Optional.ofNullable(payee.get("residenceState")).map(JsonNode::asText).orElse("");

        // Validate and convert residenceState
        String residenceStateText = UNKNOWN;
        if (!residenceState.isEmpty()) {
            try {
                int residenceStateCode = Integer.parseInt(residenceState);
                residenceStateText = ResidenceStateUtil.getStateName(residenceStateCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid residence state code: {}. Using default '{}'", residenceState, UNKNOWN, e);
            }
        }

        BigDecimal settlementInterestAmt = Optional.ofNullable(dest.get("settlementInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
        BigDecimal lateInterestAmt = Optional.ofNullable(dest.get("lateInterestAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
        BigDecimal deathBenefitPayoutAmt = Optional.ofNullable(dest.get("deathBenefitPayoutAmt")).map(JsonNode::decimalValue).orElse(BigDecimal.ZERO);
        String partyNumber = Optional.ofNullable(payee.get("partyNumber")).map(JsonNode::asText).orElse("");

        // Check if firstName and lastName are UNKNOWN
        String organization = UNKNOWN;
        if (UNKNOWN.equals(firstName) && UNKNOWN.equals(lastName)) {
            JsonNode organizationNode = Optional.ofNullable(payee.get("organization"))
                    .map(org -> org.get("dba"))
                    .orElse(null);
            if (organizationNode != null) {
                organization = organizationNode.asText();
            }
        }

        return Collections.singletonList(Arrays.asList(
                productCode,
                polNumber,
                partyNumber,
                firstName,
                lastName,
                organization, // Moved "Organization" after "Last Name"
                residenceStateText,
                formatDate(transEffDate),
                formatBigDecimal(settlementInterestAmt),
                formatBigDecimal(lateInterestAmt),
                formatBigDecimal(deathBenefitPayoutAmt)
        ));
    }


    private String getText(JsonNode node, String... path) {
        JsonNode current = node;
        for (String p : path) {
            current = current.get(p);
            if (current == null) return UNKNOWN;
        }
        return current.asText();
    }

    private BigDecimal getDecimalValue(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        return valueNode == null ? BigDecimal.ZERO : valueNode.decimalValue();
    }

    private String getStateName(String residenceState) {
        try {
            int residenceStateCode = Integer.parseInt(residenceState);
            return ResidenceStateUtil.getStateName(residenceStateCode);
        } catch (NumberFormatException e) {
            logger.warn("Invalid residence state code: {}", residenceState, e);
            return UNKNOWN;
        }
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) return "";
        DecimalFormat df = new DecimalFormat(CURRENCY_FORMAT);
        return df.format(value);
    }

    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL).format(date);
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
            return baos.toByteArray();
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Product Code", "Policy Number", "Party Id", "First Name", "Last Name",
                "Organization", "Residence State", "Transaction Effective Date", "Settlement Interest Amount",
                "Late Interest Amount", "Gross Amount"
        };
        IntStream.range(0, headers.length).forEach(i -> {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        });
    }

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

    private void autoSizeColumns(Sheet sheet) {
        int numberOfColumns = sheet.getRow(0).getLastCellNum();
        IntStream.range(0, numberOfColumns).forEach(sheet::autoSizeColumn);
    }
}
