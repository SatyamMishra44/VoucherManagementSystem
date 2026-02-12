package com.example.Voucher.controller;

import com.example.Voucher.dto.VoucherRedemptionHistoryDto;
import com.example.Voucher.dto.VoucherRedemptionRequestDto;
import com.example.Voucher.dto.VoucherRedemptionResponseDto;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.UserService;
import com.example.Voucher.service.VoucherRedemptionService;
import com.example.Voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/redemptions")
public class VoucherRedemptionController {

    private final VoucherRedemptionService voucherRedemptionService;
    private final VoucherService voucherService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    public VoucherRedemptionController(
            VoucherRedemptionService voucherRedemptionService,
            VoucherService voucherService,
            UserService userService,
            CurrentUserService currentUserService
    ) {
        this.voucherRedemptionService = voucherRedemptionService;
        this.voucherService = voucherService;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    public ResponseEntity<VoucherRedemptionResponseDto> redeemVoucher(
            @Valid @RequestBody VoucherRedemptionRequestDto requestDto) {

        try {
            Long userId = currentUserService.getCurrentUserId();
            voucherRedemptionService.redeemVoucher(
                    userId,
                    requestDto.getVoucherCode(),
                    requestDto.getBillAmount()
            );

            Optional<Voucher> voucherOpt = voucherService.getVoucherByCode(requestDto.getVoucherCode());
            if (voucherOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Voucher voucher = voucherOpt.get();
            double discountAmount =
                    (requestDto.getBillAmount() * voucher.getDiscountPercentage()) / 100.0;

            VoucherRedemptionResponseDto response = new VoucherRedemptionResponseDto();
            response.setVoucherCode(voucher.getCode());
            response.setOriginalBillAmount(requestDto.getBillAmount());
            response.setDiscountPercentage(voucher.getDiscountPercentage());
            response.setDiscountAmount(discountAmount);
            response.setFinalPayableAmount(requestDto.getBillAmount() - discountAmount);
            response.setVoucherApplied(true);
            response.setMessage("Voucher redeemed successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            VoucherRedemptionResponseDto response = new VoucherRedemptionResponseDto();
            response.setVoucherCode(requestDto.getVoucherCode());
            response.setOriginalBillAmount(requestDto.getBillAmount());
            response.setVoucherApplied(false);
            response.setMessage(ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }



    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    public ResponseEntity<List<VoucherRedemptionHistoryDto>> getRedemptionsByUserId(
            @PathVariable Long userId) {

        currentUserService.assertSelfOrAdmin(userId);
        if (!userService.existsById(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<VoucherRedemptionHistoryDto> history = voucherRedemptionService
                .getRedemptionsByUserId(userId)
                .stream()
                .map(VoucherRedemptionHistoryDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
}
