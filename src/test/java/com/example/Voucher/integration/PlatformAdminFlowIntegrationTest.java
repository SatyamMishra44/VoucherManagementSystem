package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.VoucherTemplateCreateRequestDto;
import com.example.Voucher.dto.VoucherTemplateResponseDto;
import com.example.Voucher.dto.onboarding.TenantOnboardingDecisionRequestDto;
import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.dto.onboarding.TenantSignupRequestDto;
import com.example.Voucher.dto.platform.TenantAuditLogResponseDto;
import com.example.Voucher.dto.platform.TenantResponseDto;
import com.example.Voucher.dto.platform.TenantStatusUpdateRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class PlatformAdminFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void platformAdmin_canGetOnboardingRequestDetails() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
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

        String tenantCode = uniqueTenantCode("ORG");
        TenantSignupRequestDto signup = new TenantSignupRequestDto();
        signup.setTenantName("Org " + tenantCode);
        signup.setTenantCode(tenantCode);
        signup.setAdminFirstName("Org");
        signup.setAdminLastName("Admin");
        signup.setAdminEmail(uniqueEmail("org-admin"));
        signup.setAdminPhoneNumber(uniquePhone());
        signup.setAdminPassword("AdminPass123!");

        ResponseEntity<TenantOnboardingRequestResponseDto> submitResponse = restTemplate.exchange(
                "/api/v1/tenant-onboarding/requests",
                HttpMethod.POST,
                new HttpEntity<>(signup, jsonHeaders(null)),
                TenantOnboardingRequestResponseDto.class
        );
        Assertions.assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TenantOnboardingRequestResponseDto submitted = submitResponse.getBody();
        Assertions.assertThat(submitted).isNotNull();

        ResponseEntity<TenantOnboardingRequestResponseDto> detailResponse = restTemplate.exchange(
                "/api/v1/platform/onboarding/requests/" + submitted.getRequestId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                TenantOnboardingRequestResponseDto.class
        );
        Assertions.assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(detailResponse.getBody()).isNotNull();
        Assertions.assertThat(detailResponse.getBody().getRequestId()).isEqualTo(submitted.getRequestId());
        Assertions.assertThat(detailResponse.getBody().getTenantCode()).isEqualTo(tenantCode);
    }

    @Test
    void platformAdmin_canApproveOnboarding_andAuditLogsRecorded() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
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

        String tenantCode = uniqueTenantCode("ORG");
        TenantSignupRequestDto signup = new TenantSignupRequestDto();
        signup.setTenantName("Org " + tenantCode);
        signup.setTenantCode(tenantCode);
        signup.setAdminFirstName("Org");
        signup.setAdminLastName("Admin");
        signup.setAdminEmail(uniqueEmail("org-admin"));
        signup.setAdminPhoneNumber(uniquePhone());
        signup.setAdminPassword("AdminPass123!");
        signup.setNotes("Integration test onboarding request");

        ResponseEntity<TenantOnboardingRequestResponseDto> submitResponse = restTemplate.exchange(
                "/api/v1/tenant-onboarding/requests",
                HttpMethod.POST,
                new HttpEntity<>(signup, jsonHeaders(null)),
                TenantOnboardingRequestResponseDto.class
        );
        Assertions.assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TenantOnboardingRequestResponseDto submitted = submitResponse.getBody();
        Assertions.assertThat(submitted).isNotNull();
        Long requestId = submitted.getRequestId();

        ResponseEntity<List<TenantOnboardingRequestResponseDto>> listResponse = restTemplate.exchange(
                "/api/v1/platform/onboarding/requests?status=PENDING",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<TenantOnboardingRequestResponseDto>>() {}
        );
        Assertions.assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(listResponse.getBody())
                .extracting(TenantOnboardingRequestResponseDto::getRequestId)
                .contains(requestId);

        TenantOnboardingDecisionRequestDto decision = new TenantOnboardingDecisionRequestDto();
        decision.setApproved(true);
        decision.setComment("Approved for integration test");

        ResponseEntity<TenantOnboardingRequestResponseDto> approveResponse = restTemplate.exchange(
                "/api/v1/platform/onboarding/requests/" + requestId + "/decision",
                HttpMethod.PATCH,
                new HttpEntity<>(decision, authHeaders(platformAuth.getAccessToken(), null)),
                TenantOnboardingRequestResponseDto.class
        );
        Assertions.assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TenantOnboardingRequestResponseDto approved = approveResponse.getBody();
        Assertions.assertThat(approved).isNotNull();
        Assertions.assertThat(approved.getStatus()).isEqualTo("APPROVED");
        Assertions.assertThat(approved.getApprovedTenantId()).isNotNull();
        Assertions.assertThat(approved.getApprovedTenantAdminUserId()).isNotNull();

        Long tenantId = approved.getApprovedTenantId();

        ResponseEntity<List<TenantResponseDto>> tenantsResponse = restTemplate.exchange(
                "/api/v1/platform/tenants",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<TenantResponseDto>>() {}
        );
        Assertions.assertThat(tenantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(tenantsResponse.getBody())
                .extracting(TenantResponseDto::getId)
                .contains(tenantId);

        TenantStatusUpdateRequestDto statusUpdate = new TenantStatusUpdateRequestDto();
        statusUpdate.setActive(false);
        statusUpdate.setReason("Integration test deactivation");

        ResponseEntity<TenantResponseDto> statusResponse = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(statusUpdate, authHeaders(platformAuth.getAccessToken(), null)),
                TenantResponseDto.class
        );
        Assertions.assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(statusResponse.getBody()).isNotNull();
        Assertions.assertThat(statusResponse.getBody().isActive()).isFalse();

        ResponseEntity<List<TenantAuditLogResponseDto>> auditResponse = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId + "/audit-logs",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<TenantAuditLogResponseDto>>() {}
        );
        Assertions.assertThat(auditResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(auditResponse.getBody()).isNotNull();
        Assertions.assertThat(auditResponse.getBody())
                .extracting(TenantAuditLogResponseDto::getAction)
                .contains("TENANT_ONBOARDING_APPROVED", "TENANT_DEACTIVATED");
    }

    @Test
    void platformAdmin_canCreateAndDisableVoucherTemplate() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
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

        VoucherTemplateCreateRequestDto request = new VoucherTemplateCreateRequestDto();
        request.setCode("TPL-" + System.currentTimeMillis());
        request.setUnitValue(new BigDecimal("100.00"));
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setExpiryDate(LocalDate.now().plusDays(10));

        ResponseEntity<VoucherTemplateResponseDto> createResponse = restTemplate.exchange(
                "/api/v1/admin/vouchers",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(platformAuth.getAccessToken(), null)),
                VoucherTemplateResponseDto.class
        );
        Assertions.assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        VoucherTemplateResponseDto created = createResponse.getBody();
        Assertions.assertThat(created).isNotNull();
        Assertions.assertThat(created.getEnabled()).isTrue();

        ResponseEntity<VoucherTemplateResponseDto> disableResponse = restTemplate.exchange(
                "/api/v1/admin/vouchers/" + created.getId() + "/status?enabled=false",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                VoucherTemplateResponseDto.class
        );
        Assertions.assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(disableResponse.getBody()).isNotNull();
        Assertions.assertThat(disableResponse.getBody().getEnabled()).isFalse();
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
