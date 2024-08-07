package com.eqh.application.controller;
import com.eqh.application.service.TransactionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/transactions")
public class TransactionHistoryController {

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @GetMapping("/generate-report")
    public ResponseEntity<String> generateReport() {
        try {
            transactionHistoryService.generateReport();
            return ResponseEntity.ok("Report generated successfully.");
        } catch (IOException e) {
            // Log the exception (optional)
            return ResponseEntity.status(500).body("Failed to generate report: " + e.getMessage());
        }
    }
}
