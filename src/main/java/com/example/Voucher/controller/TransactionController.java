package com.example.Voucher.controller;

import com.example.Voucher.dto.TransactionCreateRequestDto;
import com.example.Voucher.dto.TransactionResponseDto;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.service.TransactionService;
import com.example.Voucher.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionCreateRequestDto requestDto) {

        User user = userService.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        int totalAmount = requestDto.getTransactionAmount().intValue();
        Transaction transaction = new Transaction(user, totalAmount, totalAmount);
        Transaction savedTransaction = transactionService.createTransaction(transaction);

        return new ResponseEntity<>(toResponse(savedTransaction), HttpStatus.CREATED);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long transactionId) {
        Optional<Transaction> transactionOpt = transactionService.getTransactionById(transactionId);
        return transactionOpt
                .map(transaction -> ResponseEntity.ok(toResponse(transaction)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByUserId(@PathVariable Long userId) {
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
        dto.setTransactionAmount(transaction.getTotalAmount().doubleValue());
        dto.setTransactionTime(transaction.getCreatedAt());
        return dto;
    }
}
