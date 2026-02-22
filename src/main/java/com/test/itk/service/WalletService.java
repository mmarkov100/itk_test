package com.test.itk.service;

import com.test.itk.dto.WalletOperationRequest;
import com.test.itk.dto.WalletResponse;
import com.test.itk.entity.OperationType;
import com.test.itk.entity.Wallet;
import com.test.itk.exception.InsufficientFundsException;
import com.test.itk.exception.WalletNotFoundException;
import com.test.itk.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void processOperation(WalletOperationRequest request) {

        Wallet wallet = walletRepository.findByIdForUpdate(request.walletId()).orElseThrow(() -> new WalletNotFoundException("Этот кошелек не существует"));

        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (request.operationType() == OperationType.WITHDRAW) {
            if (wallet.getBalance().compareTo(request.amount()) < 0)
                throw new InsufficientFundsException("Недостаточно средств. На счету: " + wallet.getBalance());

            wallet.setBalance(wallet.getBalance().subtract(request.amount()));
            return;
        }
        wallet.setBalance(wallet.getBalance().add(request.amount()));
    }

    public WalletResponse getWallet (UUID id){

        Wallet wallet = walletRepository.findById(id).orElseThrow(() -> new WalletNotFoundException("Этот кошелек не существует"));
        return new WalletResponse(wallet.getId(), wallet.getBalance());
    }
}
