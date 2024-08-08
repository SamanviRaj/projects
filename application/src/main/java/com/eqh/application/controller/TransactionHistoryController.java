package com.eqh.application.controller;

import com.eqh.application.service.TransactionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/transactions")
public class TransactionHistoryController {

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @GetMapping("/generate-report")
    public ResponseEntity<byte[]> generateReport() {
        try {
            byte[] reportBytes = transactionHistoryService.generateReportAsBytes();

            String timestamp = new SimpleDateFormat("dd-MM-yyyy_HHmmss").format(new Date());
            String filename = "transaction_history_death_claim_report_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
