package com.example.Voucher.dto;

import com.example.Voucher.entity.VoucherTemplate;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VoucherTemplateResponseDto {

    private Long id;
    private String code;
    private BigDecimal unitValue;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public static VoucherTemplateResponseDto fromEntity(VoucherTemplate template) {
        VoucherTemplateResponseDto dto = new VoucherTemplateResponseDto();
        dto.setId(template.getId());
        dto.setCode(template.getCode());
        dto.setUnitValue(template.getUnitValue());
        dto.setStartDate(template.getStartDate());
        dto.setExpiryDate(template.getExpiryDate());
        dto.setEnabled(template.getEnabled());
        dto.setCreatedAt(template.getCreatedAt());
        return dto;
    }
}
