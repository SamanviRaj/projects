package com.eqh.application.service;

import com.eqh.application.dto.Address;
import com.eqh.application.dto.ProcessedRow;
import com.eqh.application.dto.ProductInfo;
import com.eqh.application.dto.ytdResponse;
import com.eqh.application.entity.PayoutPaymentHistory;
import com.eqh.application.feignClient.PartyClient;
import com.eqh.application.repository.*;
import com.eqh.application.utility.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class PeriodicPayoutTransactionHistoryDateRangeService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryDateRangeService.class);
    private static final String DATE_FORMAT = "MMddyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "MM-dd-yyyy";
    private static final String DATE_FORMAT_FOR_DISPLAY = "MM/dd/yyyy";
    private static final String CURRENCY_FORMAT = "$#,##0.00";
    private static final String[] HEADERS = {
            "runYear", "transRunDate","transExeDate","Management Code",  "Product Code", "polNumber", "Policy Status",
            "QualPlanType","Suspend Code","Party ID", "Party Full Name","Govt ID", "Govt ID Status",
            "govt ID Type Code","payeeStatus", "Residence State" , "Residence Country", "preferredMailingAddress",
            "mailingAddress",   "YTD Gross amount" ,"YTD fedral amount ","YTD State amount"
    };
    private static final String TWENTY_CONSTANT = "20";
    private static final String TWENTY_ONE_CONSTANT = "21";
    private static final String ZERO_CONSTANT = "0";
    private static final String TWO_CONSTANT = "2";
    private static final String ONE_CONSTANT = "1";
    private static final String THREE_CONSTANT = "3";
    private final PeriodicPayoutTransactionHistoryRepository repository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat excelDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_EXCEL);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_DISPLAY);
    private final DecimalFormat currencyFormat = new DecimalFormat(CURRENCY_FORMAT);
    private final PartyClient partyClient;
    private final PayoutPaymentHistoryRepository payoutPaymentHistoryRepository;
    private final PolicyPayoutRepository policyPayoutRepository;
    private final PayoutPayeeRepository payoutPayeeRepository;
    private final PayoutPaymentHistoryAdjustmentRepository payoutPaymentHistoryAdjustmentRepository;
    private final PayoutPaymentHistoryDeductionRepository payoutPaymentHistoryDeductionRepository;
    private final ProcessedRow processedRow = new ProcessedRow();
    private ytdResponse ytres = new ytdResponse();
    @Value("${payout.start.date}")
    private String startDate;
    @Value("${payout.transaction.start.date}")
    private String payoutTransExectStartDate;

    @Value("${payout.transaction.end.date}")
    private String payoutTransExectEndDate;



    @Autowired
    public PeriodicPayoutTransactionHistoryDateRangeService(
            PeriodicPayoutTransactionHistoryRepository repository,
            PolicyRepository policyRepository,
            ObjectMapper objectMapper,
            PartyClient partyClient,
            PayoutPaymentHistoryRepository payoutPaymentHistoryRepository,
            PolicyPayoutRepository policyPayoutRepository,
            PayoutPayeeRepository payoutPayeeRepository,
            PayoutPaymentHistoryAdjustmentRepository payoutPaymentHistoryAdjustmentRepository,
            PayoutPaymentHistoryDeductionRepository payoutPaymentHistoryDeductionRepository) {
        this.repository = repository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
        this.partyClient = partyClient;
        this.payoutPaymentHistoryRepository = payoutPaymentHistoryRepository;
        this.policyPayoutRepository = policyPayoutRepository;
        this.payoutPayeeRepository = payoutPayeeRepository;
        this.payoutPaymentHistoryAdjustmentRepository = payoutPaymentHistoryAdjustmentRepository;
        this.payoutPaymentHistoryDeductionRepository = payoutPaymentHistoryDeductionRepository;
    }

    public byte[] getMessageImagesAsJson() throws IOException {
//        LocalDateTime startDate = LocalDateTime.of(2024, 7, 15, 0, 0);
        LocalDateTime startDate = LocalDateTime.of(2021, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2022, 1, 1, 0, 0);
        List<Object[]> transactions = repository.findPayoutTransactionsInRange(startDate, endDate);

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
                                    return objectMapper.readTree(jsonObject.toString());
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
        LocalDateTime startRangeDate = LocalDateTime.parse(payoutTransExectStartDate);
        LocalDateTime endRangeDate = LocalDateTime.parse(payoutTransExectEndDate);
        // Convert startDate to LocalDate
        List<Object[]> data = repository.findLatestTransactions(startRangeDate, endRangeDate);

        logger.info("Fetching transactions history size: " + data.size() + " for date: " + startRangeDate);
        if (data.isEmpty()) {
            throw new IOException("No data found for the report.");
        }

        // Extract unique policy numbers
        Set<String> policyNumbers = extractPolicyNumbers(data);
        // Fetch product info for all policy numbers
        Map<String, ProductInfo> productInfoMap = fetchProductInfoForPolicyNumbers(policyNumbers);
        // Extract unique taxable party numbers
        Set<String> uniqueTaxablePartyNumbers = extractUniqueTaxablePartyNumbers(data);
        // Fetch mailing addresses for unique taxable party numbers
        Map<String, Map<String, String>> mailingAddressesMap = fetchMailingAddressesForTaxablePartyNumbers(uniqueTaxablePartyNumbers);
        // Prepare YTD response object
        ytdResponse ytdObj = new ytdResponse();
        // Combine all payouts from all policy numbers
        List<Object[]> periodicPayoutUponPolNumber = new ArrayList<>();
        List<Object[]> overduePeriodicPayoutUponPolNumber = new ArrayList<>();

        for(Map.Entry<String, ProductInfo> entry : productInfoMap.entrySet()) {
            periodicPayoutUponPolNumber.addAll(repository.findPayoutTransactionsByPolicyNumber(entry.getKey(),startRangeDate,endRangeDate));
//            overduePeriodicPayoutUponPolNumber.addAll(repository.findOverduePaymentsByPolicyNumberAndDate(entry.getKey(), startRangeDate));
        }

/*        for (String policyNumber : policyNumbers) {
            periodicPayoutUponPolNumber.addAll(repository.findPayoutTransactionsByPolicyNumber(policyNumber, startRangeDate));
        }*/

        if (periodicPayoutUponPolNumber.isEmpty()) {
            throw new IOException("No payout transactions found for the specified policies.");
        }

        // Transform data
        List<List<Object>> transformedData = periodicPayoutUponPolNumber.stream()
                .map(row -> processRow(row, productInfoMap, mailingAddressesMap, ytdObj))
                .collect(Collectors.toList());

        // Generate timestamp
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        // Generate and return the Excel report as bytes
        return generateExcelReportAsBytes(transformedData, timestamp);
    }

    private Set<String> extractUniquePolicyNumbers(List<Object[]> data) {
        return data.stream()
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
    }

    private Set<String> extractPolicyNumbers(List<Object[]> data) {
        Set<String> policyNumbers = new HashSet<>();
        for (Object[] row : data) {
            // Assuming the first element is the policy number
            String policyNumber = (String) row[0]; // Adjust index as necessary
            policyNumbers.add(policyNumber);
        }
        return policyNumbers; // Return the Set directly
    }

    private Set<String> extractUniqueTaxablePartyNumbers(List<Object[]> data) {
        return data.stream()
                .flatMap(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[3]);
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
                        return taxablePartyNumbers.stream(); // Return a stream of taxable party numbers
                    } catch (IOException e) {
                        logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
                        return Stream.empty(); // Return an empty stream on error
                    }
                })
                .collect(Collectors.toSet()); // Collect unique taxable party numbers
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
                        arr -> new ProductInfo((String) arr[0],(String) arr[1], (String) arr[2], (String) arr[3], (String) arr[4]) // managementCode, policyStatus, productCode
                ));
    }

    private List<Object> processRow(Object[] row, Map<String, ProductInfo> productInfoMap, Map<String, Map<String, String>> mailingAddressesMap, ytdResponse ytdObj) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree((String) row[0]);
        } catch (IOException e) {
            logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
            return Collections.singletonList("Error processing JSON");
        }

        String polNumber = jsonNode.path("polNumber").asText();
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("","", "", "", ""));

        String transExeDate = convertDateString(jsonNode.path("transExeDate").asText());
        String transRunDate = convertDateString(jsonNode.path("transRunDate").asText());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(transRunDate, formatter);
        String runYear = String.valueOf(date.getYear());
        String suspendCode = jsonNode.path("suspendCode").asText();

        // Initialize variables for aggregated or extracted data
        BigDecimal federalNonTaxableAmt = BigDecimal.ZERO;
        BigDecimal grossAmt = BigDecimal.ZERO;
        BigDecimal modalBenefit = BigDecimal.ZERO;
        LocalDateTime startRangeDate = LocalDateTime.parse(payoutTransExectStartDate);
        LocalDateTime endRangeDate = LocalDateTime.parse(payoutTransExectEndDate);
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
        String transformedPayeeStatus = null;
        if (payeeStatus != null && !payeeStatus.trim().isEmpty()) {
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

        // Retrieve addresses from the map
        Map<String, String> addressesMap = mailingAddressesMap.getOrDefault(taxablePartyNumber, new HashMap<>());
        String preferredMailingAddress = addressesMap.getOrDefault("preferredAddress", "");
        String mailingAddress = addressesMap.getOrDefault("mailingAddress", "");


        LocalDateTime payoutTransExecdate = LocalDateTime.parse(payoutTransExectStartDate);
        ytres.setYtdDisbursePeriodicPayout(BigDecimal.ZERO);//Sum of gross amunt
        ytres.setYtdDisburseFederalWithholdingAmt(BigDecimal.ZERO);//Sum of fedral Withholdingamount
        ytres.setYtdDisburseStateWithholdingAmt(BigDecimal.ZERO);//Sum of state Withholding amunt
        Double totalGrossAmount = 0.0;
        Double totalFeeAmtForState = 0.0;
        Double totalFeeAmtForFederal = 0.0;
        Double totalAdjustmentValueForState = 0.0;
        Double totalAdjustmentValueForFederal = 0.0;
        Calendar ytdCalendar = Calendar.getInstance();
        ytdCalendar.set(Calendar.YEAR, 2025);
        ytdCalendar.set(Calendar.MONTH, Calendar.AUGUST); // Note: Months are zero-based (0 = January, 6 = July)
        ytdCalendar.set(Calendar.DATE, 21);

        Double policyPayoutsGrossAmt = payoutPaymentHistoryRepository.findPolicyPayoutWithGrossAmount(productInfo.getPolNumber(), startRangeDate,endRangeDate);

        totalFeeAmtForFederal = payoutPaymentHistoryDeductionRepository.sumFeeAmtByFeeTypeFederal(taxablePartyNumber, startRangeDate,endRangeDate, productInfo.getPolNumber());
        totalFeeAmtForState = payoutPaymentHistoryDeductionRepository.sumFeeAmtByFeeTypeState(taxablePartyNumber,startRangeDate,endRangeDate,productInfo.getPolNumber());

        totalAdjustmentValueForFederal = payoutPaymentHistoryAdjustmentRepository.sumAdjustmentValueByFieldAdjustmentFederal(taxablePartyNumber,startRangeDate,endRangeDate ,productInfo.getPolNumber());
        totalAdjustmentValueForState = payoutPaymentHistoryAdjustmentRepository.sumAdjustmentValueByFieldAdjustmentState(taxablePartyNumber,startRangeDate,endRangeDate ,productInfo.getPolNumber());
        List<PayoutPaymentHistory> policyPayouts = payoutPaymentHistoryRepository.findPolicyPayouts(productInfo.getPolNumber(),startRangeDate,endRangeDate);

        Double finalTotalGrossAmount = policyPayoutsGrossAmt;
        Double finalTotalFeeAmtForFederal = totalFeeAmtForFederal;
        Double finalTotalFeeAmtForState = totalFeeAmtForState;

        Double finalTotalAdjustmentValueForFederal = totalAdjustmentValueForFederal;
        Double finalTotalAdjustmentValueForState = totalAdjustmentValueForState;

        if(finalTotalGrossAmount != null && finalTotalGrossAmount > 0) {
            ytres.setYtdDisbursePeriodicPayout(BigDecimal.valueOf(finalTotalGrossAmount));
        }

        policyPayouts.forEach(policyPayout -> {
               /* This checks if the payout due date is on or before the transaction effective date and is on or after the year start
            If this part is true, it means the payout is either due today or has already passed.
           and
        This checks if the payout due date is on or after the start of the year (or whatever date ytdCalendar.getTime() represents).
            If this part is true, it means the payout is due any time from the beginning of the year up to today.*/
//            if (policyPayout.getPayoutDueDate().compareTo(transEffDate) <= 0 &&
//                    policyPayout.getPayoutDueDate().compareTo(ytdCalendar.getTime()) >= 0) {

            if((finalTotalFeeAmtForFederal != null && finalTotalFeeAmtForFederal > 0) || (finalTotalAdjustmentValueForFederal != null && finalTotalAdjustmentValueForFederal > 0)) {
                ytres.setYtdDisburseFederalWithholdingAmt(BigDecimal.valueOf(finalTotalFeeAmtForFederal + finalTotalAdjustmentValueForFederal));
            }
            if((finalTotalFeeAmtForState != null && finalTotalFeeAmtForState > 0) || (finalTotalAdjustmentValueForState != null && finalTotalAdjustmentValueForState > 0)) {
                ytres.setYtdDisburseStateWithholdingAmt(BigDecimal.valueOf(finalTotalFeeAmtForState + finalTotalAdjustmentValueForState));
            }
//             }
        });

        return Arrays.asList(
                runYear,
                transRunDate,
                transExeDate,
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
                mailingAddress,
                ytres.getYtdDisbursePeriodicPayout(),
                ytres.getYtdDisburseFederalWithholdingAmt(),
                ytres.getYtdDisburseStateWithholdingAmt()
        );
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
    private String formatDate(Date date) {
        // Create a SimpleDateFormat with the desired format
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MM/dd/yyyy");

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

}