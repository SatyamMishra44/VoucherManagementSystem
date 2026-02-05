package com.example.Voucher.controller;



import com.example.Voucher.dto.VoucherCreateRequestDto;
import com.example.Voucher.dto.VoucherResponseDto;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.service.VoucherService;
import com.example.Voucher.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    private final VoucherService voucherService;
    private final UserService userService;

    public VoucherController(VoucherService voucherService, UserService userService) {
        this.voucherService = voucherService;
        this.userService = userService;
    }

    // ===================== ADMIN OPERATIONS =====================

    // Create a new voucher (Admin)
    @PostMapping
    public ResponseEntity<VoucherResponseDto> createVoucher(
            @Valid @RequestBody VoucherCreateRequestDto requestDto) {

        User createdBy = userService.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Voucher voucher = requestDto.toEntity(createdBy);
        Voucher savedVoucher = voucherService.createVoucher(voucher);

        return new ResponseEntity<>(
                VoucherResponseDto.fromEntity(savedVoucher),
                HttpStatus.CREATED
        );
    }

    // Enable / Disable voucher (Admin)
    @PatchMapping("/{voucherId}/status")
    public ResponseEntity<VoucherResponseDto> updateVoucherStatus(
            @PathVariable Long voucherId,
            @RequestParam boolean enabled) {

        Voucher updatedVoucher =
                voucherService.updateVoucherStatus(voucherId, enabled);

        return ResponseEntity.ok(
                VoucherResponseDto.fromEntity(updatedVoucher)
        );
    }

    // ===================== USER OPERATIONS =====================

    // Get voucher by ID
    @GetMapping("/{voucherId}")
    public ResponseEntity<VoucherResponseDto> getVoucherById(
            @PathVariable Long voucherId) {

        Optional<Voucher> voucherOpt =
                voucherService.getVoucherById(voucherId);

        return voucherOpt
                .map(voucher -> ResponseEntity.ok(
                        VoucherResponseDto.fromEntity(voucher)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get voucher by code
    @GetMapping("/code/{code}")
    public ResponseEntity<VoucherResponseDto> getVoucherByCode(
            @PathVariable String code) {

        Optional<Voucher> voucherOpt =
                voucherService.getVoucherByCode(code);

        return voucherOpt
                .map(voucher -> ResponseEntity.ok(
                        VoucherResponseDto.fromEntity(voucher)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get all vouchers (eligible listing)
    @GetMapping
    public ResponseEntity<List<VoucherResponseDto>> getAllVouchers() {

        List<VoucherResponseDto> vouchers =
                voucherService.getAllVouchers()
                        .stream()
                        .map(VoucherResponseDto::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(vouchers);
    }

    // ===================== TRANSACTION SUPPORT =====================

    // Validate voucher before redemption
    @GetMapping("/validate/{code}")
    public ResponseEntity<VoucherResponseDto> validateVoucher(
            @PathVariable String code) {

        Voucher voucher = voucherService.validateVoucher(code);

        return ResponseEntity.ok(
                VoucherResponseDto.fromEntity(voucher)
        );
    }
}
