package com.test.itk.controller;

import com.test.itk.dto.WalletOperationRequest;
import com.test.itk.entity.Wallet;
import com.test.itk.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallet")
    public ResponseEntity<?> processOperation(@Valid @RequestBody WalletOperationRequest request) {

        walletService.processOperation(request);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/wallets/{id}")
    public ResponseEntity<?> getWallet (@PathVariable UUID id) {
        Wallet wallet = walletService.getWallet(id);
        return ResponseEntity.ok(Map.of(
                "walletId", wallet.getId(),
                "balance", wallet.getBalance()));
    }
}
