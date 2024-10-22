package com.eqh.application.controller;

import com.eqh.application.service.PeriodicPayoutTransactionHistoryDateRangeService;
import com.eqh.application.service.PeriodicPayoutTransactionHistoryService;
import com.eqh.application.service.TransactionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/transactions")
public class TransactionHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryController.class);
    private static final String TIMESTAMP_FORMAT = "MM-dd-yyyy_HHmmss";
    private static final String FILENAME_PREFIX = "transaction_history_death_claim_report_";
    private static final String PERIODIC_PAYOUT_FILENAME_PREFIX = "transaction_history_periodic_payout_report_";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final String JSON_FILENAME_PREFIX = "transaction_history_data_";
    private static final String JSON_FILE_EXTENSION = ".json";

    private final TransactionHistoryService transactionHistoryService;

    private final PeriodicPayoutTransactionHistoryService periodicPayoutTransactionHistoryService;

    private final PeriodicPayoutTransactionHistoryDateRangeService periodicPayoutTransactionHistoryDateRangeService;

    @Autowired
    public TransactionHistoryController(TransactionHistoryService transactionHistoryService,PeriodicPayoutTransactionHistoryService periodicPayoutTransactionHistoryService,
    PeriodicPayoutTransactionHistoryDateRangeService periodicPayoutTransactionHistoryDateRangeService) {
        this.transactionHistoryService = transactionHistoryService;
        this.periodicPayoutTransactionHistoryService = periodicPayoutTransactionHistoryService;
        this.periodicPayoutTransactionHistoryDateRangeService = periodicPayoutTransactionHistoryDateRangeService;
    }

    @GetMapping("deathclaim/generate-report")
    public ResponseEntity<byte[]> generateReport() {
        try {
            byte[] reportBytes = transactionHistoryService.generateReportAsBytes();

            String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
            String filename = FILENAME_PREFIX + timestamp + FILE_EXTENSION;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error generating report", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("periodicpayout/generate-report")
    public ResponseEntity<byte[]> periodicPayoutgenerateReport() {
        try {
            byte[] reportBytes = periodicPayoutTransactionHistoryService.generateReportAsBytes();

            String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
            String filename = PERIODIC_PAYOUT_FILENAME_PREFIX + timestamp + FILE_EXTENSION;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error generating report", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("periodicpayout/dateRange/generate-report")
    public ResponseEntity<byte[]> periodicPayoutgenerateDateRangeReport() {
        try {
            byte[] reportBytes = periodicPayoutTransactionHistoryDateRangeService.generateReportAsBytes();

            String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
            String filename = PERIODIC_PAYOUT_FILENAME_PREFIX + timestamp + FILE_EXTENSION;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error generating report", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download-json")
    public ResponseEntity<byte[]> downloadJson() {
        try {
            byte[] jsonBytes = transactionHistoryService.getJsonDataAsBytes();

            String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
            String filename = JSON_FILENAME_PREFIX + timestamp + JSON_FILE_EXTENSION;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error downloading JSON data", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint to download message_image data as a JSON file.
     *
     * @param
     * @return A ResponseEntity with the JSON file.
     */
    @GetMapping("periodicpayout/download-json")
    public ResponseEntity<byte[]> downloadMessageImages() {
        try {
            byte[] jsonBytes = periodicPayoutTransactionHistoryService.getMessageImagesAsJson();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "message_images.json");

            return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
