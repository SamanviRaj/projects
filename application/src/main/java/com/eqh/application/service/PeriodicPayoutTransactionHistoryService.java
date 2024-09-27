package com.eqh.application.service;

import com.eqh.application.dto.Address;
import com.eqh.application.dto.ProcessedRow;
import com.eqh.application.dto.ProductInfo;
import com.eqh.application.dto.ytdResponse;
import com.eqh.application.entity.*;
import com.eqh.application.repository.*;
import com.eqh.application.utility.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hpsf.Decimal;
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
import com.eqh.application.feignClient.PartyClient;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class PeriodicPayoutTransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicPayoutTransactionHistoryService.class);
    private static final String DATE_FORMAT = "ddMMyyyy_HHmmss";
    private static final String DATE_FORMAT_FOR_EXCEL = "yyyy-MM-dd";
    private static final String DATE_FORMAT_FOR_DISPLAY = "dd/MM/yyyy";
    private static final String CURRENCY_FORMAT = "$#,##0.00";


    private static final String[] HEADERS = {
            "runYear", "transRunDate","Management Code",  "Product Code", "polNumber", "Policy Status",
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

    @Value("${transaction.start.date}")
    private String payoutTransExectDate;

    @Autowired
    public PeriodicPayoutTransactionHistoryService(
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

//        LocalDate localStartDate = LocalDate.of(2024, 7, 15); // Adjust to the date you are querying
//        LocalDateTime startDate = LocalDateTime.of(2021, 1, 1, 0, 0);
//        LocalDateTime endDate = LocalDateTime.of(2022, 1, 1, 0, 0);

        LocalDate startRangeDate = LocalDate.parse(startDate);
        List<Object[]> data = repository.findPayoutTransactionsInRange(startRangeDate.atStartOfDay());
        logger.info("fetching transactions history size "+data.size()+" for date "+startRangeDate);
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

        // Extract unique taxablePartyNumbers
        Set<String> uniqueTaxablePartyNumbers = data.stream()
                .map(row -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree((String) row[0]);
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
                        return taxablePartyNumbers;
                    } catch (IOException e) {
                        logger.error("Error parsing JSON for row: " + Arrays.toString(row), e);
                        return new HashSet<String>();
                    }
                })
                .flatMap(Set::stream) // Flatten the set of sets
                .collect(Collectors.toSet()); // Collect unique taxablePartyNumbers

        // Print unique taxablePartyNumbers
        System.out.println("Unique Taxable Party Numbers:");
        uniqueTaxablePartyNumbers.forEach(System.out::println);

        Map<String, Map<String, String>> mailingAddressesMap = fetchMailingAddressesForTaxablePartyNumbers(uniqueTaxablePartyNumbers);

        ytdResponse ytdObj = new ytdResponse();

        // Transform data
        List<List<Object>> transformedData = data.stream()
                .map(row -> processRow(row, productInfoMap, mailingAddressesMap,ytdObj))
                .collect(Collectors.toList());

        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        return generateExcelReportAsBytes(transformedData, timestamp);
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
                        arr -> new ProductInfo((String) arr[1], (String) arr[2], (String) arr[3], (String) arr[4]) // managementCode, policyStatus, productCode
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
        ProductInfo productInfo = productInfoMap.getOrDefault(polNumber, new ProductInfo("", "", "", ""));

        Date transEffDate = parseDate(jsonNode.path("transEffDate").asText());
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

        // Fetch Policy by polNumber
        Long policyId = policyRepository.findPolicyByNumberAndStatus(polNumber);
        Long policyPayoutId = policyPayoutRepository.findPolicyPayoutByPolicyId(policyId);

        // Fetch PayoutPayee by taxablePartyNumber
//        List<PayoutPayee>  payoutPayee = payoutPayeeRepository.findPayoutPayeeByPolicyPayoutIdAndPartyNumber(policyPayoutId);

        PayoutPayee payoutPayeeObj = new PayoutPayee();
        List<PayoutPayee> payoutPayeeList = payoutPayeeRepository.findPayoutPayeeByPolicyPayoutIdAndPartyNumber(policyPayoutId);
        for (PayoutPayee payoutPayees : payoutPayeeList) {
            if (payoutPayees.getPayeePartyNumber().equalsIgnoreCase(String.valueOf(taxablePartyNumber))) {
                try {
                    payoutPayeeObj = payoutPayees;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        LocalDate payoutTransExecdate = LocalDate.parse(payoutTransExectDate);
        // Fetch PayoutPaymentHistory by policyPayoutId and payoutPayeeId
        List<PayoutPaymentHistory> payoutPaymentHistoryList = payoutPaymentHistoryRepository.findPayoutPaymentHistoryByPayeePartyNumberAndPayeeId(String.valueOf(taxablePartyNumber), payoutPayeeObj.getId(),payoutTransExecdate.atStartOfDay());

        List<PayoutPaymentHistory> payoutPaymentHistoryLists = payoutPaymentHistoryList.stream()
                .filter(m -> !m.getReversed()).sorted(Comparator.comparingLong(PayoutPaymentHistory::getId))
                .collect(Collectors.toList());

        logger.info("fetching payoutPaymentHistoryLists size "+payoutPaymentHistoryList.size()+" for date "+payoutTransExecdate);

        List<Long> paymentHistoryAdjustmentIds = payoutPaymentHistoryAdjustmentRepository.findAllPaymentHistoryIdsOfAdjustments();
        List<Long> paymentHistoryDeductionIds = payoutPaymentHistoryDeductionRepository.findAllPaymentHistoryIdsOfDeductions();


        System.out.println("polNumber :: "+polNumber);
        System.out.println("policyRepository.findPolicyByNumberAndStatus(polNumber)  :: "+policyId);
        System.out.println("policyPayoutRepository.findPolicyPayoutByPolicyId(policyId)  :: "+policyPayoutId);
        System.out.println("taxablePartyNumber :: "+taxablePartyNumber+" payoutPayee "+ payoutPayeeObj.toString());
        payoutPaymentHistoryLists.forEach(pph -> System.out.println("payoutPaymentHistoryLists :: "+pph.toString()));

        ytres.setYtdDisbursePeriodicPayout(BigDecimal.ZERO);
        ytres.setYtdDisburseFederalWithholdingAmt(BigDecimal.ZERO);
        ytres.setYtdDisburseStateWithholdingAmt(BigDecimal.ZERO);


        payoutPaymentHistoryLists.forEach(pph -> {
            if (!"1000500003".equalsIgnoreCase(pph.getPayeeStatus()) && pph.getPayoutDueDate() != null) {
                boolean isAdjustmentId = paymentHistoryAdjustmentIds.contains(pph.getId());
                boolean isDeductionId = paymentHistoryDeductionIds.contains(pph.getId());

                if (isAdjustmentId || isDeductionId) {
                    List<PayoutPaymentHistoryAdjustment> payoutPaymentHistoryAdjustment = payoutPaymentHistoryAdjustmentRepository.findFeeDetailsByPayoutPaymentHistoryId(pph.getId());
                    List<PayoutPaymentHistoryDeduction> payoutPaymentHistoryDeduction = payoutPaymentHistoryDeductionRepository.findFeeDetailsByPayoutPaymentHistoryId(pph.getId());
                    payoutPaymentHistoryAdjustment.forEach(ppd -> System.out.println("payoutPaymentHistoryAdjustment :: "+ppd.toString()));
                    payoutPaymentHistoryDeduction.forEach(ppd -> System.out.println("payoutPaymentHistoryDeduction :: "+ppd.toString()));
                    // Call the new ytdCalculation method
                    ytres = ytdCalculation(transEffDate, pph, payoutPaymentHistoryDeduction, payoutPaymentHistoryAdjustment, ytdObj);
                }
            }
        });


        return Arrays.asList(
                runYear,
                formatDate(transRunDate),
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

    private ytdResponse ytdCalculation(Date transEffDate,
                                       PayoutPaymentHistory pph,
                                       List<PayoutPaymentHistoryDeduction> deductions,
                                       List<PayoutPaymentHistoryAdjustment> adjustments,
                                       ytdResponse ytd) {

        // Initialize YTD values from ytdResponse
        BigDecimal ytdCurrentPayoutAmount = Optional.ofNullable(ytd.getYtdDisbursePeriodicPayout()).orElse(BigDecimal.ZERO);
        BigDecimal ytdFederalAmount = Optional.ofNullable(ytd.getYtdDisburseFederalWithholdingAmt()).orElse(BigDecimal.ZERO);
        BigDecimal ytdStateAmount = Optional.ofNullable(ytd.getYtdDisburseStateWithholdingAmt()).orElse(BigDecimal.ZERO);
        BigDecimal ytdInterestAmount = Optional.ofNullable(ytd.getYtdDisburseInterest()).orElse(BigDecimal.ZERO);

        // Set calendar to the beginning of the year for comparison
        Calendar ytdCalendar = Calendar.getInstance();
        ytdCalendar.set(Calendar.YEAR, 2024);
        ytdCalendar.set(Calendar.MONTH, Calendar.JULY); // Note: Months are zero-based (0 = January, 6 = July)
        ytdCalendar.set(Calendar.DATE, 31);

        /* This checks if the payout due date is on or before the transaction effective date and is on or after the year start
            If this part is true, it means the payout is either due today or has already passed.
           and
        This checks if the payout due date is on or after the start of the year (or whatever date ytdCalendar.getTime() represents).
            If this part is true, it means the payout is due any time from the beginning of the year up to today.*/
        if (pph.getPayoutDueDate().compareTo(transEffDate) <= 0 &&
                pph.getPayoutDueDate().compareTo(ytdCalendar.getTime()) >= 0) {

            // Update current payout amount
            ytdCurrentPayoutAmount = ytdCurrentPayoutAmount.add(pph.getGrossAmt());

            // Process deductions
            for (PayoutPaymentHistoryDeduction deduction : deductions) {
                if (TWENTY_CONSTANT.equalsIgnoreCase(deduction.getFeeType())) {
                    ytdFederalAmount = ytdFederalAmount.add(deduction.getFeeAmt());
                } else if (TWENTY_ONE_CONSTANT.equalsIgnoreCase(deduction.getFeeType())) {
                    ytdStateAmount = ytdStateAmount.add(deduction.getFeeAmt());
                }
            }

            // Process adjustments
            for (PayoutPaymentHistoryAdjustment adjustment : adjustments) {
                String fieldAdjustment = adjustment.getFieldAdjustment();
                String adjustmentType = adjustment.getAdjustmentType();
                BigDecimal adjustmentValue = adjustment.getAdjustmentValue();

                switch (fieldAdjustment) {
                    case ZERO_CONSTANT: // Gross adjustments
                        if (TWO_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdCurrentPayoutAmount = ytdCurrentPayoutAmount.add(adjustmentValue);
                        } else if (ONE_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdCurrentPayoutAmount = ytdCurrentPayoutAmount.subtract(adjustmentValue);
                        }
                        break;

                    case ONE_CONSTANT: // Interest adjustments
                        if (TWO_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdInterestAmount = ytdInterestAmount.add(adjustmentValue);
                        } else if (ONE_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdInterestAmount = ytdInterestAmount.subtract(adjustmentValue);
                        }
                        break;

                    case TWO_CONSTANT: // Federal adjustments
                        if (TWO_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdFederalAmount = ytdFederalAmount.add(adjustmentValue);
                        } else if (ONE_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdFederalAmount = ytdFederalAmount.subtract(adjustmentValue);
                        }
                        break;

                    case THREE_CONSTANT: // State adjustments
                        if (TWO_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdStateAmount = ytdStateAmount.add(adjustmentValue);
                        } else if (ONE_CONSTANT.equalsIgnoreCase(adjustmentType)) {
                            ytdStateAmount = ytdStateAmount.subtract(adjustmentValue);
                        }
                        break;

                    default:
                        // Handle unexpected field adjustments if necessary
                        break;
                }
            }
        }

        System.out.println("gross amount"+ytdCurrentPayoutAmount);
        System.out.println("FederalAmount"+ytdFederalAmount);
        System.out.println("StateAmount"+ytdStateAmount);
        System.out.println("InterestAmount"+ytdInterestAmount);

        // Set calculated values in the response object
        ytd.setYtdDisbursePeriodicPayout(ytdCurrentPayoutAmount);
        ytd.setYtdDisburseFederalWithholdingAmt(ytdFederalAmount);
        ytd.setYtdDisburseStateWithholdingAmt(ytdStateAmount);
        ytd.setYtdDisburseInterest(ytdInterestAmount);
        return ytd;
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

}
