package com.example.Voucher.controller;

import com.example.Voucher.dto.TransactionResponseDto;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @PreAuthorize("hasAnyAuthority(@roleProperties.getPlatformAdmin(), @roleProperties.getTenantAdmin(), @roleProperties.getUser())")
    @Operation(
            summary = "View Transactions By User",
            description = "Get payment/settlement history for a user. Users can view only their own transactions."
    )
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByUserId(@PathVariable Long userId) {
        currentUserService.assertSelfOrAdmin(userId);
        List<TransactionResponseDto> transactions = transactionService.getTransactionByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(@roleProperties.getPlatformAdmin(), @roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Admin: Filter Transactions",
            description = "Admin-only endpoint to filter transactions by user, amount ranges, and time window."
    )
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsWithFilters(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BigDecimal minTotalAmount,
            @RequestParam(required = false) BigDecimal maxTotalAmount,
            @RequestParam(required = false) BigDecimal minFinalAmount,
            @RequestParam(required = false) BigDecimal maxFinalAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime
    ) {
        List<TransactionResponseDto> transactions = transactionService.getTransactionsWithFilters(
                        userId,
                        minTotalAmount,
                        maxTotalAmount,
                        minFinalAmount,
                        maxFinalAmount,
                        fromTime,
                        toTime
                ).stream()
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
        dto.setFinalAmount(transaction.getFinalAmount());
        dto.setTransactionTime(transaction.getCreatedAt());
        return dto;
    }
}
