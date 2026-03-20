package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.UserVoucherResponseDto;
import com.example.Voucher.dto.VoucherPurchaseRequestDto;
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

class VoucherViewsIntegrationTest extends IntegrationTestBase {

    @Test
    void user_canViewOwnVouchers_andAdminCanFilterIssuedVouchers() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
        Role userRole = roleByName(roleProperties.getUser());

        String adminPassword = "AdminPass123!";
        User platformAdmin = createUser(
                "Platform",
                "Admin",
                uniqueEmail("platform-admin"),
                uniquePhone(),
                adminPassword,
                platformAdminRole,
                systemTenant
        );
        AuthResponseDto platformAuth = login(platformAdmin.getEmail(), adminPassword, null);

        String userPassword = "UserPass123!";
        User user = createUser(
                "System",
                "User",
                uniqueEmail("user"),
                uniquePhone(),
                userPassword,
                userRole,
                systemTenant
        );
        AuthResponseDto userAuth = login(user.getEmail(), userPassword, null);

        VoucherTemplate template = createVoucherTemplate("VCH-" + System.currentTimeMillis(), systemTenant);

        VoucherPurchaseRequestDto purchaseRequest = new VoucherPurchaseRequestDto();
        purchaseRequest.setVoucherCode(template.getCode());
        purchaseRequest.setQuantity(1);

        ResponseEntity<UserVoucherResponseDto> purchaseResponse = restTemplate.exchange(
                "/api/v1/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, authHeaders(userAuth.getAccessToken(), null)),
                UserVoucherResponseDto.class
        );
        Assertions.assertThat(purchaseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserVoucherResponseDto purchased = purchaseResponse.getBody();
        Assertions.assertThat(purchased).isNotNull();

        ResponseEntity<List<UserVoucherResponseDto>> myVouchersResponse = restTemplate.exchange(
                "/api/v1/vouchers/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<UserVoucherResponseDto>>() {}
        );
        Assertions.assertThat(myVouchersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(myVouchersResponse.getBody())
                .extracting(UserVoucherResponseDto::getId)
                .contains(purchased.getId());

        ResponseEntity<List<UserVoucherResponseDto>> adminIssuedResponse = restTemplate.exchange(
                "/api/v1/vouchers/admin/issued?assignedUserId=" + user.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<UserVoucherResponseDto>>() {}
        );
        Assertions.assertThat(adminIssuedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(adminIssuedResponse.getBody())
                .extracting(UserVoucherResponseDto::getId)
                .contains(purchased.getId());
    }
}
