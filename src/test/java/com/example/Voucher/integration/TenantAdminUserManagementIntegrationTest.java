package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.UserResponseDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.tenant.Tenant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TenantAdminUserManagementIntegrationTest extends IntegrationTestBase {

    @Test
    void tenantAdmin_canListUsersWithinTenantOnly() {
        Tenant orgA = createOrganizationTenant(uniqueTenantCode("ORG"));
        Tenant orgB = createOrganizationTenant(uniqueTenantCode("ORG"));

        Role tenantAdminRole = roleByName(roleProperties.getTenantAdmin());
        Role userRole = roleByName(roleProperties.getUser());

        String adminPassword = "AdminPass123!";
        User adminA = createUser(
                "Tenant",
                "Admin",
                uniqueEmail("tenant-admin"),
                uniquePhone(),
                adminPassword,
                tenantAdminRole,
                orgA
        );
        User userA = createUser(
                "Tenant",
                "UserA",
                uniqueEmail("user"),
                uniquePhone(),
                "UserPass123!",
                userRole,
                orgA
        );
        User userB = createUser(
                "Tenant",
                "UserB",
                uniqueEmail("user"),
                uniquePhone(),
                "UserPass123!",
                userRole,
                orgB
        );

        AuthResponseDto tenantAdminAuth = login(adminA.getEmail(), adminPassword, orgA.getTenantCode());

        ResponseEntity<List<UserResponseDto>> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenantAdminAuth.getAccessToken(), orgA.getTenantCode())),
                new ParameterizedTypeReference<List<UserResponseDto>>() {}
        );

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody())
                .extracting(UserResponseDto::getEmail)
                .contains(adminA.getEmail(), userA.getEmail())
                .doesNotContain(userB.getEmail());
    }
}
