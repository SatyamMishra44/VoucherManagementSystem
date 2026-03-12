package com.example.Voucher.controller;

import com.example.Voucher.dto.RedemptionHistoryResponseDto;
import com.example.Voucher.dto.UserVoucherResponseDto;
import com.example.Voucher.dto.VoucherPurchaseRequestDto;
import com.example.Voucher.dto.VoucherRedeemRequestDto;
import com.example.Voucher.dto.VoucherRedeemResponseDto;
import com.example.Voucher.dto.VoucherTemplateResponseDto;
import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.RedemptionResult;
import com.example.Voucher.service.UserVoucherService;
import com.example.Voucher.service.VoucherTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "4. User Vouchers", description = "User voucher listing, purchase, redemption and history APIs")
@RestController
@RequestMapping("/api/v1/vouchers")
public class UserVoucherController {

    private final VoucherTemplateService voucherTemplateService;
    private final UserVoucherService userVoucherService;
    private final CurrentUserService currentUserService;

    public UserVoucherController(VoucherTemplateService voucherTemplateService,
                                 UserVoucherService userVoucherService,
                                 CurrentUserService currentUserService) {
        this.voucherTemplateService = voucherTemplateService;
        this.userVoucherService = userVoucherService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(@roleProperties.getUser(), @roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Step 4 (User): View Eligible Voucher Templates",
            description = "See templates that are currently active and within valid date range."
    )
    public ResponseEntity<List<VoucherTemplateResponseDto>> getEligibleVoucherTemplates() {
        List<VoucherTemplate> templates = voucherTemplateService.getEligibleTemplates();
        List<VoucherTemplateResponseDto> response = templates.stream()
                .map(VoucherTemplateResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority(@roleProperties.getUser())")
    @Operation(
            summary = "Step 5 (User): Purchase Voucher",
            description = "Buy a voucher using template code and quantity. System creates your personal voucher balance."
    )
    public ResponseEntity<UserVoucherResponseDto> purchaseVoucher(
            @Valid @RequestBody VoucherPurchaseRequestDto request
    ) {
        Long userId = currentUserService.getCurrentUserId();
        UserVoucher userVoucher = userVoucherService.purchaseVoucher(userId, request.getVoucherCode(), request.getQuantity());
        return ResponseEntity.ok(UserVoucherResponseDto.fromEntity(userVoucher));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasAuthority(@roleProperties.getUser())")
    @Operation(
            summary = "Step 6 (User): Redeem Voucher Balance",
            description = "Apply your voucher balance to a bill amount. Supports partial redemption and records an audit entry."
    )
    public ResponseEntity<VoucherRedeemResponseDto> redeemVoucher(
            @Valid @RequestBody VoucherRedeemRequestDto request
    ) {
        Long userId = currentUserService.getCurrentUserId();
        RedemptionResult result = userVoucherService.redeemVoucher(
                userId,
                request.getUserVoucherId(),
                request.getBillId()
        );

        VoucherRedeemResponseDto response = new VoucherRedeemResponseDto();
        response.setRedemptionId(result.getRedemptionId());
        response.setUserVoucherId(result.getUserVoucher().getId());
        response.setBillId(result.getBillId());
        response.setBillAmount(result.getBillAmount());
        response.setRedeemedAmount(result.getRedeemedAmount());
        response.setPayableAmount(result.getPayableAmount());
        response.setRemainingBalance(result.getUserVoucher().getRemainingBalance());
        response.setUserVoucherStatus(result.getUserVoucher().getStatus().name());
        response.setRedeemedAt(result.getRedeemedAt());
        response.setMessage("Voucher redeemed successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority(@roleProperties.getUser())")
    @Operation(
            summary = "User: View My Purchased Vouchers",
            description = "See your purchased vouchers with status and remaining balance."
    )
    public ResponseEntity<List<UserVoucherResponseDto>> getMyVouchers() {
        Long userId = currentUserService.getCurrentUserId();
        List<UserVoucherResponseDto> response = userVoucherService.getUserVouchers(userId)
                .stream()
                .map(UserVoucherResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/redemptions")
    @PreAuthorize("hasAuthority(@roleProperties.getUser())")
    @Operation(
            summary = "Step 7 (User): View My Redemption History",
            description = "Get complete redemption records for audit: redeemed amount, remaining balance, date, and bill reference."
    )
    public ResponseEntity<List<RedemptionHistoryResponseDto>> getMyRedemptionHistory() {
        Long userId = currentUserService.getCurrentUserId();
        List<RedemptionHistory> history = userVoucherService.getUserRedemptionHistory(userId);
        List<RedemptionHistoryResponseDto> response = history.stream()
                .map(RedemptionHistoryResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/issued")
    @PreAuthorize("hasAnyAuthority(@roleProperties.getPlatformAdmin(), @roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Admin: Filter Issued User Vouchers",
            description = "Filter issued vouchers by amount, redemption state, issued/expiry date range, status, and assigned user."
    )
    public ResponseEntity<List<UserVoucherResponseDto>> getAdminFilteredIssuedVouchers(
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) BigDecimal minVoucherAmount,
            @RequestParam(required = false) BigDecimal maxVoucherAmount,
            @RequestParam(required = false) String redemptionState,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
            @RequestParam(required = false) String status
    ) {
        List<UserVoucherResponseDto> response = userVoucherService.getAdminFilteredVouchers(
                        assignedUserId,
                        minVoucherAmount,
                        maxVoucherAmount,
                        redemptionState,
                        issuedFrom,
                        issuedTo,
                        expiryFrom,
                        expiryTo,
                        status
                ).stream()
                .map(UserVoucherResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
