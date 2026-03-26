package com.example.Voucher.tenant;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantResolverService {
    // Filter → calls Resolver → Resolver talks to DB → returns tenantId

    private final TenantRepository tenantRepository;

    public TenantResolverService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Long resolveTenantId(String tenantCodeHeader) {
        String tenantCode = StringUtils.hasText(tenantCodeHeader)
                ? tenantCodeHeader.trim()
                : TenantConstants.SYSTEM_INDIVIDUAL_CODE; // if no tenant code is provided default goes to  System_individual
        return tenantRepository.findByTenantCodeAndActiveTrue(tenantCode)
                .map(Tenant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tenant code"));
    }

    public Long requireActiveTenantId(Long tenantId) {
        return tenantRepository.findByIdAndActiveTrue(tenantId)
                .map(Tenant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tenant"));
    }

    public String normalizeTenantCode(String tenantCodeHeader) {
        if (!StringUtils.hasText(tenantCodeHeader)) { // validate tenantCode not null,not spaces
            return TenantConstants.SYSTEM_INDIVIDUAL_CODE;
        }
        return tenantCodeHeader.trim();
    }

    public Optional<Tenant> findByCode(String tenantCode) {
        return tenantRepository.findByTenantCodeAndActiveTrue(tenantCode);
    }
}
