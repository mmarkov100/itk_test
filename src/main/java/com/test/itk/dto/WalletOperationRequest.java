package com.test.itk.dto;

import com.test.itk.entity.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest(
        @NotNull UUID walletId,
        @NotNull OperationType operationType,
        @NotNull @DecimalMin("0.01") BigDecimal amount
        ) { }
