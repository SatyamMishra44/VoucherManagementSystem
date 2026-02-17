package com.example.Voucher.controller;

import com.example.Voucher.dto.TransactionResponseDto;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "6. Transactions", description = "Transaction APIs")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public TransactionController(
            TransactionService transactionService,
            CurrentUserService currentUserService
    ) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByUserId(@PathVariable Long userId) {
        currentUserService.assertSelfOrAdmin(userId);
        List<TransactionResponseDto> transactions = transactionService.getTransactionByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    private TransactionResponseDto toResponse(Transaction transaction) {
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setTransactionId(transaction.getId());
        dto.setUserId(transaction.getUser().getId());
        dto.setBillId(transaction.getBill().getId());
        dto.setTransactionAmount(transaction.getTotalAmount());
        dto.setTransactionTime(transaction.getCreatedAt());
        return dto;
    }
}
