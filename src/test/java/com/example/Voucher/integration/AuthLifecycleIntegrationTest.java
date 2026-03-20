package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.LogoutRequestDto;
import com.example.Voucher.dto.RefreshTokenRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AuthLifecycleIntegrationTest extends IntegrationTestBase {

    @Test
    void user_canRefreshToken_andLogoutInvalidatesRefreshToken() {
        Tenant systemTenant = systemTenant();
        Role userRole = roleByName(roleProperties.getUser());

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

        AuthResponseDto loginAuth = login(user.getEmail(), userPassword, null);
        Assertions.assertThat(loginAuth.getRefreshToken()).isNotBlank();

        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken(loginAuth.getRefreshToken());

        ResponseEntity<AuthResponseDto> refreshResponse = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest, jsonHeaders(null)),
                AuthResponseDto.class
        );
        Assertions.assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponseDto refreshed = refreshResponse.getBody();
        Assertions.assertThat(refreshed).isNotNull();
        Assertions.assertThat(refreshed.getAccessToken()).isNotBlank();
        Assertions.assertThat(refreshed.getAccessToken()).isNotEqualTo(loginAuth.getAccessToken());

        LogoutRequestDto logoutRequest = new LogoutRequestDto();
        logoutRequest.setRefreshToken(loginAuth.getRefreshToken());

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest, jsonHeaders(null)),
                Void.class
        );
        Assertions.assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshAfterLogout = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest, jsonHeaders(null)),
                String.class
        );
        Assertions.assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders jsonHeaders(String tenantCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (tenantCode != null) {
            headers.add(TenantConstants.TENANT_HEADER, tenantCode);
        }
        return headers;
    }
}
