package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.BillCreateRequestDto;
import com.example.Voucher.dto.BillResponseDto;
import com.example.Voucher.dto.UserVoucherResponseDto;
import com.example.Voucher.dto.VoucherPurchaseRequestDto;
import com.example.Voucher.dto.VoucherRedeemRequestDto;
import com.example.Voucher.dto.VoucherRedeemResponseDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import java.math.BigDecimal;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotencyIntegrationTest extends IntegrationTestBase {

    @Test
    void billCreation_isIdempotent() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());

        String adminPassword = "AdminPass123!";
        User platformAdmin = createUser("Platform", "Admin", uniqueEmail("admin"), uniquePhone(), adminPassword,
                platformAdminRole, systemTenant);
        AuthResponseDto platformAuth = login(platformAdmin.getEmail(), adminPassword, null);

        User user = createUser("Test", "User", uniqueEmail("testuser"), uniquePhone(), "UserPass123!",
                roleByName(roleProperties.getUser()), systemTenant);

        BillCreateRequestDto billRequest = new BillCreateRequestDto();
        billRequest.setUserId(user.getId());
        billRequest.setTotalAmount(new BigDecimal("100.00"));

        String requestId = UUID.randomUUID().toString();
        HttpHeaders headers = authHeaders(platformAuth.getAccessToken(), null);
        headers.set(TenantConstants.X_REQUEST_ID_HEADER, requestId);

        // First request
        ResponseEntity<BillResponseDto> response1 = restTemplate.exchange(
                "/api/v1/bills",
                HttpMethod.POST,
                new HttpEntity<>(billRequest, headers),
                BillResponseDto.class);
        Assertions.assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BillResponseDto bill1 = response1.getBody();
        Assertions.assertThat(bill1).isNotNull();

        // Second request with same requestId
        ResponseEntity<BillResponseDto> response2 = restTemplate.exchange(
                "/api/v1/bills",
                HttpMethod.POST,
                new HttpEntity<>(billRequest, headers),
                BillResponseDto.class);
        // Should return 201 (as it returns the SAME object) or 200 depending on
        // implementation.
        // My implementation returns the same object in service, and Controller returns
        // CREATED on POST.
        Assertions.assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BillResponseDto bill2 = response2.getBody();
        Assertions.assertThat(bill2).isNotNull();
        Assertions.assertThat(bill2.getBillId()).isEqualTo(bill1.getBillId());
    }

    @Test
    void voucherPurchase_isIdempotent() {
        Tenant systemTenant = systemTenant();
        User user = createUser("Voucher", "Buyer", uniqueEmail("buyer"), uniquePhone(), "Pass123!",
                roleByName(roleProperties.getUser()), systemTenant);
        AuthResponseDto userAuth = login(user.getEmail(), "Pass123!", null);
        VoucherTemplate template = createVoucherTemplate("VCH-" + System.currentTimeMillis(), systemTenant);

        VoucherPurchaseRequestDto purchaseRequest = new VoucherPurchaseRequestDto();
        purchaseRequest.setVoucherCode(template.getCode());
        purchaseRequest.setQuantity(1);

        String requestId = UUID.randomUUID().toString();
        HttpHeaders headers = authHeaders(userAuth.getAccessToken(), null);
        headers.set(TenantConstants.X_REQUEST_ID_HEADER, requestId);

        // First purchase
        ResponseEntity<UserVoucherResponseDto> response1 = restTemplate.exchange(
                "/api/v1/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, headers),
                UserVoucherResponseDto.class);
        Assertions.assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserVoucherResponseDto v1 = response1.getBody();

        // Second purchase with same requestId
        ResponseEntity<UserVoucherResponseDto> response2 = restTemplate.exchange(
                "/api/v1/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, headers),
                UserVoucherResponseDto.class);
        Assertions.assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserVoucherResponseDto v2 = response2.getBody();
        Assertions.assertThat(v2.getId()).isEqualTo(v1.getId());
    }

    @Test
    void voucherRedemption_isIdempotent() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
        User platformAdmin = createUser("Platform", "Admin", uniqueEmail("admin2"), uniquePhone(), "AdminPass123!",
                platformAdminRole, systemTenant);
        AuthResponseDto platformAuth = login(platformAdmin.getEmail(), "AdminPass123!", null);

        User user = createUser("Voucher", "Redeemer", uniqueEmail("redeemer"), uniquePhone(), "Pass123!",
                roleByName(roleProperties.getUser()), systemTenant);
        AuthResponseDto userAuth = login(user.getEmail(), "Pass123!", null);
        VoucherTemplate template = createVoucherTemplate("VCH-RED-" + System.currentTimeMillis(), systemTenant);

        // Purchase first
        VoucherPurchaseRequestDto purchaseRequest = new VoucherPurchaseRequestDto();
        purchaseRequest.setVoucherCode(template.getCode());
        purchaseRequest.setQuantity(2);
        UserVoucherResponseDto purchased = restTemplate.exchange(
                "/api/v1/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, authHeaders(userAuth.getAccessToken(), null)),
                UserVoucherResponseDto.class).getBody();

        // Create bill
        BillCreateRequestDto billRequest = new BillCreateRequestDto();
        billRequest.setUserId(user.getId());
        billRequest.setTotalAmount(new BigDecimal("15.00"));
        BillResponseDto bill = restTemplate.exchange(
                "/api/v1/bills",
                HttpMethod.POST,
                new HttpEntity<>(billRequest, authHeaders(platformAuth.getAccessToken(), null)),
                BillResponseDto.class).getBody();

        VoucherRedeemRequestDto redeemRequest = new VoucherRedeemRequestDto();
        redeemRequest.setUserVoucherId(purchased.getId());
        redeemRequest.setBillId(bill.getBillId());

        String requestId = UUID.randomUUID().toString();
        HttpHeaders headers = authHeaders(userAuth.getAccessToken(), null);
        headers.set(TenantConstants.X_REQUEST_ID_HEADER, requestId);

        // First redemption
        ResponseEntity<VoucherRedeemResponseDto> response1 = restTemplate.exchange(
                "/api/v1/vouchers/redeem",
                HttpMethod.POST,
                new HttpEntity<>(redeemRequest, headers),
                VoucherRedeemResponseDto.class);
        Assertions.assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        VoucherRedeemResponseDto r1 = response1.getBody();

        // Second redemption with same requestId
        ResponseEntity<VoucherRedeemResponseDto> response2 = restTemplate.exchange(
                "/api/v1/vouchers/redeem",
                HttpMethod.POST,
                new HttpEntity<>(redeemRequest, headers),
                VoucherRedeemResponseDto.class);
        Assertions.assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        VoucherRedeemResponseDto r2 = response2.getBody();
        Assertions.assertThat(r2.getRedemptionId()).isEqualTo(r1.getRedemptionId());
    }
}
