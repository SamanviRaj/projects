package com.eqh.application.service;

import com.eqh.application.dto.Address;
import com.eqh.application.dto.Party;
import com.eqh.application.dto.Person;
import com.eqh.application.dto.ProductInfo;
import com.eqh.application.feignClient.PartyClient;
import com.eqh.application.repository.PolicyRepository;
import com.eqh.application.repository.TransactionHistoryRepository;
import com.eqh.application.utility.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
public class OverduePaymentTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String CURRENCY_FORMAT = "$#,##0.00";
    private static final String ERROR_PROCESSING_ROW = "Error processing row";
    private static final String UNKNOWN = "Unknown";

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;
    private final PartyClient partyClient;


    public OverduePaymentTransactionHistoryService(TransactionHistoryRepository transactionHistoryRepository, PolicyRepository policyRepository, ObjectMapper objectMapper, PartyClient partyClient) {
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
        this.partyClient = partyClient;
    }


    /**
     * Generates a report in Excel format and returns it as a byte array.
     *
     * @return byte array containing the Excel report.
     * @throws IOException if an error occurs during data retrieval or report generation.
     */
    public byte[] generateReportAsBytes() throws IOException {
        List<Object[]> data = transactionHistoryRepository.findOverduePaymentTransactions();

        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Load all policy-product mappings in advance to optimize processing
        Map<String, String> policyProductCodes = loadPolicyProductCodes();

        // Extract unique policy numbers
        Set<String> policyNumbers = extractPolicyNumbers(data);

        // Fetch product info for all policy numbers
        Map<String, ProductInfo> productInfoMap = fetchProductInfoForPolicyNumbers(policyNumbers);

        // Filter out rows with policy status "R"
        List<Object[]> filteredData = data.stream()
                .filter(row -> {
                    String policyNumber = (String) row[4];  // Assuming policy number is at index 4
                    ProductInfo productInfo = productInfoMap.get(policyNumber);

                    // Keep the row if the policy status is not "R"
                    return productInfo == null || !"R".equals(productInfo.getPolicyStatus());
                })
                .collect(Collectors.toList());


//        // Transform raw data into a format suitable for Excel
//        List<List<Object>> transformedData = data.stream()
//                .flatMap(row -> processRow(row, policyProductCodes, productInfoMap).stream())
//                .collect(Collectors.toList());

        // Transform the filtered data into a format suitable for Excel
        List<List<Object>> transformedData = filteredData.stream()
                .flatMap(row -> processRow(row, policyProductCodes, productInfoMap).stream())
                .collect(Collectors.toList());


        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        // Generate the Excel report
        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    /**
     * Loads policy numbers and their corresponding product codes from the repository.
     *
     * @return a map of policy numbers to product codes.
     */
    private Map<String, String> loadPolicyProductCodes() {
        List<Object[]> policies = policyRepository.findAllPolicyNumbersWithProductCodesOverduePayment();
        return policies.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // Policy Number
                        row -> (String) row[1]  // Product Code
                ));
    }

    /**
     * Processes a single row of transaction data.
     *
     * @param row                the raw data row.
     * @param policyProductCodes map of policy numbers to product codes.
     * @param productInfoMap
     * @return a list of lists containing transformed data for each row.
     */
    private List<List<Object>> processRow(Object[] row, Map<String, String> policyProductCodes, Map<String, ProductInfo> productInfoMap) {
        List<List<Object>> result = new ArrayList<>();
        try {
            // Parse JSON
            JsonNode jsonMap = objectMapper.readTree((String) row[0]);

            // Extract policy number and look up product code
            String polNumber = Optional.ofNullable(jsonMap.get("polNumber")).map(JsonNode::asText).orElse(UNKNOWN);
            String productCode = policyProductCodes.getOrDefault(polNumber, UNKNOWN);
            String transExeDate = Optional.ofNullable(jsonMap.get("transExeDate")).map(JsonNode::asText).orElse(UNKNOWN);
            String suspendCode = Optional.ofNullable(jsonMap.get("suspendCode")).map(JsonNode::asText).orElse(UNKNOWN);



            // Extract the 'adjustments' node
            JsonNode adjustments = jsonMap.get("adjustments");
            if (adjustments == null) {
                return Collections.emptyList();  // Exit if no adjustments node found
            }

            // Extract 'arrDestinations' node
            JsonNode arrDestinations = adjustments.get("arrDestinations");
            if (arrDestinations == null || !arrDestinations.isArray()) {
                return Collections.emptyList();  // Exit if no arrDestinations found
            }

            // Process each jsonMap
            result.addAll(StreamSupport.stream(arrDestinations.spliterator(), false)
                    .map(dest -> {
                        return processDestinationOverduePayment(dest, (BigDecimal) row[1], (Date) row[2], (Date) row[3], transExeDate, polNumber, productCode, productInfoMap, suspendCode);
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            logger.error("Error processing row", e);
            result.add(Collections.singletonList("ERROR_PROCESSING_ROW"));
        }
        return result;
    }

    /**
     * Processes a single destination within a transaction.
     *
     * @param dest           the destination node.
     * @param grossAmt       the gross amount associated with the transaction.
     * @param transEffDate   the effective date of the transaction.
     * @param polNumber      the policy number.
     * @param productCode    the product code.
     * @param productInfoMap
     * @return a list of lists containing processed destination data.
     */
    private List<List<Object>> processDestinationOverduePayment(JsonNode dest, BigDecimal grossAmt, Date transEffDate, Date transRunDate, String transExecDate, String polNumber, String productCode, Map<String, ProductInfo> productInfoMap, String suspendCode) {

        // Extract payee info directly from the destination
        JsonNode payeeInfo = dest.get("payeeInfo");

        // Extract payee directly from the destination
        JsonNode payee = dest.get("payee");

        if (payeeInfo == null) {
            return Collections.emptyList();
        }

        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("","", "", "", ""));

        // Extract taxable party payee details
        String residenceState = Optional.ofNullable(payeeInfo.get("taxableToResidenceState")).map(JsonNode::asText).orElse("");

        // Validate and convert residence state
        String residenceStateText = UNKNOWN;
        if (!residenceState.isEmpty()) {
            try {
                int residenceStateCode = Integer.parseInt(residenceState);
                residenceStateText = ResidenceStateUtil.getStateName(residenceStateCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid residence state code: {}. Using default '{}'", residenceState, UNKNOWN, e);
            }
        }

        String residenceCountry = Optional.ofNullable(payeeInfo.get("taxableToResidenceCountry")).map(JsonNode::asText).orElse("");
        String  partyNumber = Optional.ofNullable(payeeInfo.get("taxablePartyNumber")).map(JsonNode::asText).orElse("");

        //Fetch Party details using Taxable party number
        Party party = new Party();
        party = partyClient.getPartyDetails(partyNumber);
        // Extract government ID and type code
        Party finalParty = party;
        String govtID = Optional.ofNullable(payeeInfo.get("taxableToGovtID"))
                .map(JsonNode::asText)
                .or(() -> Optional.ofNullable(finalParty.getGovtId())) // This acts as an 'else if'
                .orElse(UNKNOWN);

        String govtIdTC = Optional.ofNullable(payeeInfo.get("taxableToGovtIdTC")).map(JsonNode::asText).orElse(UNKNOWN);
        // Transform suspendCode using SuspendCodeUtil
        String transformedSuspendCode = SuspendCodeUtil.getSuspendCodeName(suspendCode);

        //Fetch person using Taxable party number
        Person person = new Person();
        if( partyNumber!=null){
            person = partyClient.getPersonDetails(partyNumber);
        }
        String firstName = "";
        String lastName ="";
        String partyFullName = "";
        if (payee != null && "1".equals(payee.get("partyTypeCode").asText())) {
            firstName = Optional.ofNullable(person.getFirstName()).orElse(UNKNOWN);
            lastName = Optional.ofNullable(person.getLastName()).orElse(UNKNOWN);
            partyFullName = firstName + " " + lastName;
        }
        else if(payee != null && "2".equals(payee.get("partyTypeCode").asText())){
            partyFullName = Optional.ofNullable(payeeInfo.get("taxablePartyName")).map(JsonNode::asText).orElse(UNKNOWN);
        }


        // Fetch addresses using Feign client
        List<Address> addresses = Collections.emptyList();
        if ( partyNumber != null) {
            addresses = partyClient.getAddresses( partyNumber);
        }

        // Find the preferred address and concatenate line1 and line2
        String preferredMailingAddress = addresses.stream()
                .filter(Address::getPrefAddr)
                .findFirst()
                .map(address -> (address.getLine1() != null ? "Line 1 :"+address.getLine1()+" " : "") +
                        (address.getLine2() != null ? "Line 2 :"+address.getLine2()+" " : " " ) +
                        (address.getLine3() != null ? "Line 3 :"+address.getLine3()+" " : " " ) +
                        (address.getLine4() != null ? "Line 4 :"+address.getLine4()+" " : " " ) +
                        (address.getLine5() != null ? "Line 5 :"+address.getLine5()+" " : " " ) +
                        (address.getZip() != null ? "Zip :"+address.getZip()+" " : ""))
                .orElse("");

        // Validate and convert residence state
        residenceCountry = addresses.stream()
                .filter(Address::getPrefAddr)
                .findFirst()
                .map(address -> ResidenceCountryUtil.getCountryName(Integer.parseInt(address.getAddressCountrytc() != null ? address.getAddressCountrytc() : "0"))).orElse("");


        // Find the non-preferred address and concatenate line1 and line2
        String mailingAddress = addresses.stream()
                .filter(address -> !address.getPrefAddr())
                .findFirst()
                .map(address -> (address.getLine1() != null ? "Line 1 :"+address.getLine1()+" " : "") +
                        (address.getLine2() != null ? "Line 2 :"+address.getLine2()+" " : " " ) +
                        (address.getLine3() != null ? "Line 3 :"+address.getLine3()+" " : " " ) +
                        (address.getLine4() != null ? "Line 4 :"+address.getLine4()+" " : " " ) +
                        (address.getLine5() != null ? "Line 5 :"+address.getLine5()+" " : " " ) +
                        (address.getZip() != null ? "Zip :"+address.getZip()+" " : ""))
                .orElse("");

        // Transform taxableToGovtIdTCode using GovtIdTCodeUtil
        String transformedGovtIdTCode = UNKNOWN;
        if (govtIdTC != null && !govtIdTC.trim().isEmpty() && govtIdTC!=UNKNOWN) {
            try {
                int govtIdTCode = Integer.parseInt(govtIdTC.trim());
                transformedGovtIdTCode = GovtIdTCodeUtil.getIdTCodeName(govtIdTCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid government ID type code: " + transformedGovtIdTCode, e);
            }
        }
       else if (govtIdTC.equals(UNKNOWN) && party.getGovtIdtc()!=null) {
          transformedGovtIdTCode =  GovtIdTCodeUtil.getIdTCodeName(Integer.parseInt(party.getGovtIdtc()));
       }
        else {
            logger.warn("Empty or null government ID type code: " + transformedGovtIdTCode);
        }

        String taxableToGovtIDStatus = Optional.ofNullable(payeeInfo.get("taxableToGovtIDStat")).map(JsonNode::asText).orElse(UNKNOWN);
        // Transform taxableToGovtIDStatus using GovtIDStatusUtil
        String transformedGovtIDStatus = "Unknown";
        if (taxableToGovtIDStatus != null && !taxableToGovtIDStatus.trim().isEmpty() && taxableToGovtIDStatus!=UNKNOWN) {
            try {
                int govtIDStatusCode = Integer.parseInt(taxableToGovtIDStatus.trim());
                transformedGovtIDStatus = GovtIDStatusUtil.getStatusName(govtIDStatusCode);
            } catch (NumberFormatException e) {
                logger.warn("Invalid government ID status code: " + taxableToGovtIDStatus, e);
            }
        }
       else if(taxableToGovtIDStatus.equals(UNKNOWN) && party.getGovtIdStat()!=null){
           transformedGovtIDStatus = GovtIDStatusUtil.getStatusName(Integer.parseInt(party.getGovtIdStat()));
       }
        else {
            logger.warn("Empty or null government ID status code: " + taxableToGovtIDStatus);
        }

        // Extract amounts for overdue payments
        BigDecimal federalWithholdingAmt = Optional.ofNullable(dest.get("payeeWithholdings"))
                .map(withholding -> withholding.get("federalWithholdingAmt"))
                .map(JsonNode::decimalValue)
                .orElse(BigDecimal.ZERO);
        BigDecimal stateWithholdingAmt = Optional.ofNullable(dest.get("payeeWithholdings"))
                .map(withholding -> withholding.get("stateWithholdingAmt"))
                .map(JsonNode::decimalValue)
                .orElse(BigDecimal.ZERO);


        // Extract year from transRunDate
        String runYear = transRunDate == null ? "" : new SimpleDateFormat("yyyy").format(transRunDate);

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

        // Transform payeeStatus using PayeeStatusUtil
        String payeeStatus = Optional.ofNullable(payeeInfo.get("payeeStatus")).map(JsonNode::asText).orElse(UNKNOWN);
        String transformedPayeeStatus = null;
        if (payeeStatus != null && !payeeStatus.trim().isEmpty() && payeeStatus!=UNKNOWN) {
            try {
                transformedPayeeStatus = PayeeStatusUtil.getDisplayName(Integer.parseInt(payeeStatus.trim()));
            } catch (NumberFormatException e) {
                // Handle the case where the string cannot be parsed as an integer
                // Log the error or set a default value
                System.err.println("Invalid payeeStatus value: " + payeeStatus);
                transformedPayeeStatus = "Unknown"; // Or another appropriate default value
            }
        } else {
            // Handle the case where payeeStatus is null or empty
            transformedPayeeStatus = "Unknown"; // Or another appropriate default value
        }

        String transactionExeDate = convertDateString(transExecDate);
        String transactionRunDate = convertDateString(String.valueOf(transRunDate));


        // Return the transformed data
        List<Object> processedData = Arrays.asList(
                runYear,
                transactionRunDate,
//                formatDate(transRunDate),
                transactionExeDate,
                productInfo.getManagementCode(),
                productCode,
                polNumber,
                transformedPolicyStatus,
                transformedQualPlanType,
                transformedSuspendCode,
                partyNumber,
                partyFullName,
                govtID,
                transformedGovtIDStatus,
                transformedGovtIdTCode,
                transformedPayeeStatus,
                residenceStateText,
                residenceCountry,
                preferredMailingAddress,
                mailingAddress,
                formatBigDecimal(grossAmt),
                formatBigDecimal(federalWithholdingAmt),
                formatBigDecimal(stateWithholdingAmt)

        );
        return Collections.singletonList(processedData);
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
                "runYear","transRunDate","transExeDate","Management Code","Product Code", "polNumber", "Policy Status",
                "QualPlanType", "Suspend Code", "Party ID","Party Full Name", "Govt ID", "Govt ID Status", "govt ID Type Code",
                "payeeStatus", "Residence State", "Residence Country", "preferredMailingAddress", "mailingAddress", "YTD Gross amount",
                "YTD fedral amount", "YTD State amount"

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

    private Map<String, ProductInfo> fetchProductInfoForPolicyNumbers(Set<String> policyNumbers) {
        // Convert Set to List
        List<String> policyNumberList = new ArrayList<>(policyNumbers);

        // Fetch product info for all policy numbers
        List<Object[]> productInfoList = policyRepository.findProductInfoByPolicyNumbersOverduePayment(policyNumberList);

        // Map the results to a Map for quick lookup
        return productInfoList.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0], // polNumber
                        arr -> new ProductInfo((String) arr[0],(String) arr[1], (String) arr[2], (String) arr[3], (String) arr[4]) // managementCode, policyStatus, productCode
                ));
    }

    private Set<String> extractPolicyNumbers(List<Object[]> data) {
        Set<String> policyNumbers = new HashSet<>();
        for (Object[] row : data) {
            // Assuming the fourth element is the policy number
            String policyNumber = (String) row[4]; // Adjust index as necessary
            policyNumbers.add(policyNumber);
        }
        return policyNumbers; // Return the Set directly
    }

    public String convertDateString(String inputDate) {
        // Define the input and output date formats
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");

        try {
            // Parse the input date string to a Date object
            Date date = inputFormat.parse(inputDate);
            // Format the Date to the desired output format
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Handle parsing exceptions (e.g., log or rethrow)
            e.printStackTrace();
            return null; // or throw an exception
        }
    }
}
