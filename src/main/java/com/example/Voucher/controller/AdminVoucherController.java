package com.example.Voucher.controller;

import com.example.Voucher.dto.VoucherTemplateCreateRequestDto;
import com.example.Voucher.dto.VoucherTemplateResponseDto;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.service.VoucherTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. Admin Vouchers", description = "Admin voucher template management APIs")
@RestController
@RequestMapping("/api/v1/admin/vouchers")
public class AdminVoucherController {

    private final VoucherTemplateService voucherTemplateService;

    public AdminVoucherController(VoucherTemplateService voucherTemplateService) {
        this.voucherTemplateService = voucherTemplateService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    @Operation(
            summary = "Step 3A (Admin): Create Voucher Template",
            description = "Create a reusable voucher definition (code, value, valid dates). Users can buy vouchers from this template."
    )
    public ResponseEntity<VoucherTemplateResponseDto> createVoucherTemplate(
            @Valid @RequestBody VoucherTemplateCreateRequestDto request
    ) {
        VoucherTemplate template = new VoucherTemplate(
                request.getCode(),
                request.getUnitValue(),
                request.getStartDate(),
                request.getExpiryDate()
        );
        VoucherTemplate saved = voucherTemplateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(VoucherTemplateResponseDto.fromEntity(saved));
    }

    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    @Operation(
            summary = "Step 3B (Admin): Enable/Disable Voucher Template",
            description = "Turn a voucher template ON or OFF without deleting it. Disabled templates cannot be purchased."
    )
    public ResponseEntity<VoucherTemplateResponseDto> updateVoucherTemplateStatus(
            @PathVariable Long templateId,
            @RequestParam boolean enabled
    ) {
        VoucherTemplate updated = voucherTemplateService.updateTemplateStatus(templateId, enabled);
        return ResponseEntity.ok(VoucherTemplateResponseDto.fromEntity(updated));
    }
}
