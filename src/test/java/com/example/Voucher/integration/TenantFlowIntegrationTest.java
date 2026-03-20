package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherDistributeRequestDto;
import com.example.Voucher.dto.tenant.TenantVoucherDistributionResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherInventoryResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherPurchaseRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.tenant.Tenant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TenantFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void tenantAdmin_canPurchaseDistributeAndInventoryIsUpdated() {
        Tenant orgA = createOrganizationTenant(uniqueTenantCode("ORG"));
        Tenant orgB = createOrganizationTenant(uniqueTenantCode("ORG"));
        VoucherTemplate template = createVoucherTemplate("TPL-" + System.currentTimeMillis(), systemTenant());

        Role tenantAdminRole = roleByName(roleProperties.getTenantAdmin());
        Role userRole = roleByName(roleProperties.getUser());

        String adminPassword = "AdminPass123!";
        User admin = createUser("Tenant", "Admin", uniqueEmail("admin"), uniquePhone(), adminPassword, tenantAdminRole, orgA);
        User targetUser = createUser("Tenant", "User", uniqueEmail("user"), uniquePhone(), "UserPass123!", userRole, orgA);

        AuthResponseDto auth = login(admin.getEmail(), adminPassword, orgA.getTenantCode());

        TenantVoucherPurchaseRequestDto purchase = new TenantVoucherPurchaseRequestDto();
        purchase.setVoucherCode(template.getCode());
        purchase.setQuantity(5);

        ResponseEntity<String> purchaseResponse = restTemplate.exchange(
                "/api/v1/tenant/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchase, authHeaders(auth.getAccessToken(), orgA.getTenantCode())),
                String.class
        );

        if (!HttpStatus.CREATED.equals(purchaseResponse.getStatusCode())) {
            throw new AssertionError("Purchase failed: " + purchaseResponse.getBody());
        }
        TenantVoucherInventoryResponseDto purchaseBody = readBody(purchaseResponse, TenantVoucherInventoryResponseDto.class);
        Long inventoryId = purchaseBody.getInventoryId();

        TenantVoucherDistributeRequestDto distribute = new TenantVoucherDistributeRequestDto();
        distribute.setInventoryId(inventoryId);
        distribute.setUserId(targetUser.getId());
        distribute.setQuantity(2);

        ResponseEntity<String> distributeResponse = restTemplate.exchange(
                "/api/v1/tenant/vouchers/distribute",
                HttpMethod.POST,
                new HttpEntity<>(distribute, authHeaders(auth.getAccessToken(), orgA.getTenantCode())),
                String.class
        );

        if (!HttpStatus.CREATED.equals(distributeResponse.getStatusCode())) {
            throw new AssertionError("Distribute failed: " + distributeResponse.getBody());
        }
        TenantVoucherDistributionResponseDto distributeBody = readBody(distributeResponse, TenantVoucherDistributionResponseDto.class);
        Assertions.assertThat(distributeBody.getQuantityDistributed()).isEqualTo(2);

        ResponseEntity<List<TenantVoucherInventoryResponseDto>> inventoryResponse = restTemplate.exchange(
                "/api/v1/tenant/vouchers/inventory",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.getAccessToken(), orgA.getTenantCode())),
                new ParameterizedTypeReference<List<TenantVoucherInventoryResponseDto>>() {
                }
        );

        Assertions.assertThat(inventoryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(inventoryResponse.getBody()).isNotNull();
        Assertions.assertThat(inventoryResponse.getBody())
                .filteredOn(item -> item.getInventoryId().equals(inventoryId))
                .singleElement()
                .extracting(TenantVoucherInventoryResponseDto::getQuantityAvailable)
                .isEqualTo(3);

        ResponseEntity<String> mismatchResponse = restTemplate.exchange(
                "/api/v1/tenant/vouchers/inventory",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.getAccessToken(), orgB.getTenantCode())),
                String.class
        );
        Assertions.assertThat(mismatchResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
