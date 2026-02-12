package com.example.Voucher.controller;



import com.example.Voucher.dto.VoucherCreateRequestDto;
import com.example.Voucher.dto.VoucherRedemptionHistoryDto;
import com.example.Voucher.dto.VoucherResponseDto;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.VoucherService;
import com.example.Voucher.service.VoucherRedemptionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "3. Vouchers", description = "Voucher management and eligibility APIs")
@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherRedemptionService voucherRedemptionService;
    private final CurrentUserService currentUserService;

    public VoucherController(
            VoucherService voucherService,
            VoucherRedemptionService voucherRedemptionService,
            CurrentUserService currentUserService
    ) {
        this.voucherService = voucherService;
        this.voucherRedemptionService = voucherRedemptionService;
        this.currentUserService = currentUserService;
    }

    // ===================== ADMIN OPERATIONS =====================

    // Create a new voucher (Admin)
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    @PostMapping
    public ResponseEntity<VoucherResponseDto> createVoucher(
            @Valid @RequestBody VoucherCreateRequestDto requestDto) {

        User createdBy = currentUserService.getCurrentUser();

        Voucher voucher = requestDto.toEntity(createdBy);
        Voucher savedVoucher = voucherService.createVoucher(voucher);

        return new ResponseEntity<>(
                VoucherResponseDto.fromEntity(savedVoucher),
                HttpStatus.CREATED
        );
    }

    // Enable / Disable voucher (Admin)
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
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
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
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
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
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
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    @GetMapping
    public ResponseEntity<List<VoucherResponseDto>> getAllVouchers() {

        List<VoucherResponseDto> vouchers =
                voucherService.getAllVouchers()
                        .stream()
                        .map(VoucherResponseDto::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(vouchers);
    }

    // Get redemption history for a voucher
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    @GetMapping("/{voucherId}/redemptions")
    public ResponseEntity<List<VoucherRedemptionHistoryDto>> getVoucherRedemptions(
            @PathVariable Long voucherId) {

        Optional<Voucher> voucherOpt = voucherService.getVoucherById(voucherId);
        if (voucherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<VoucherRedemptionHistoryDto> history = voucherRedemptionService
                .getRedemptionsByVoucherId(voucherId)
                .stream()
                .map(VoucherRedemptionHistoryDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    // Get eligible vouchers for a user
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    @GetMapping("/eligible")
    public ResponseEntity<List<VoucherResponseDto>> getEligibleVouchers(
            @RequestParam(required = false) Long userId) {
        Long resolvedUserId = userId != null ? userId : currentUserService.getCurrentUserId();
        currentUserService.assertSelfOrAdmin(resolvedUserId);
        List<VoucherResponseDto> vouchers =
                voucherService.getEligibleVouchers(resolvedUserId)
                        .stream()
                        .map(VoucherResponseDto::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(vouchers);
    }

    // ===================== TRANSACTION SUPPORT =====================

    // Validate voucher before redemption
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    @GetMapping("/validate/{code}")
    public ResponseEntity<VoucherResponseDto> validateVoucher(
            @PathVariable String code) {

        Voucher voucher = voucherService.validateVoucher(code);

        return ResponseEntity.ok(
                VoucherResponseDto.fromEntity(voucher)
        );
    }
}
