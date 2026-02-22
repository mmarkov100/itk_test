package com.test.itk.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(UUID id, BigDecimal balance) {
}
