package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.report.ReportJobResponseDto;
import com.example.Voucher.dto.report.SelfReportEmailRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.tenant.Tenant;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReportFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void user_canRequestReport_andPlatformAdminCanViewJobStatus() {
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

        SelfReportEmailRequestDto request = new SelfReportEmailRequestDto();
        request.setFromDate(LocalDate.now().minusDays(7));
        request.setToDate(LocalDate.now());

        ResponseEntity<ReportJobResponseDto> createResponse = restTemplate.exchange(
                "/api/v1/reports/me/email",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(userAuth.getAccessToken(), null)),
                ReportJobResponseDto.class
        );
        Assertions.assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ReportJobResponseDto createdJob = createResponse.getBody();
        Assertions.assertThat(createdJob).isNotNull();
        Assertions.assertThat(createdJob.getReportJobId()).isNotNull();
        Assertions.assertThat(createdJob.getTargetType()).isEqualTo("USER");
        Assertions.assertThat(createdJob.getTargetId()).isEqualTo(user.getId());

        ResponseEntity<ReportJobResponseDto> statusResponse = restTemplate.exchange(
                "/api/v1/reports/" + createdJob.getReportJobId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                ReportJobResponseDto.class
        );
        Assertions.assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(statusResponse.getBody()).isNotNull();
        Assertions.assertThat(statusResponse.getBody().getReportJobId())
                .isEqualTo(createdJob.getReportJobId());
    }

    @Test
    void tenantAdmin_canRequestTenantReport() {
        Tenant orgTenant = createOrganizationTenant(uniqueTenantCode("ORG"));
        Role tenantAdminRole = roleByName(roleProperties.getTenantAdmin());

        String adminPassword = "AdminPass123!";
        User tenantAdmin = createUser(
                "Tenant",
                "Admin",
                uniqueEmail("tenant-admin"),
                uniquePhone(),
                adminPassword,
                tenantAdminRole,
                orgTenant
        );
        AuthResponseDto tenantAuth = login(tenantAdmin.getEmail(), adminPassword, orgTenant.getTenantCode());

        SelfReportEmailRequestDto request = new SelfReportEmailRequestDto();
        request.setFromDate(LocalDate.now().minusDays(7));
        request.setToDate(LocalDate.now());

        ResponseEntity<ReportJobResponseDto> createResponse = restTemplate.exchange(
                "/api/v1/reports/tenant/email",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(tenantAuth.getAccessToken(), orgTenant.getTenantCode())),
                ReportJobResponseDto.class
        );
        Assertions.assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ReportJobResponseDto createdJob = createResponse.getBody();
        Assertions.assertThat(createdJob).isNotNull();
        Assertions.assertThat(createdJob.getReportJobId()).isNotNull();
        Assertions.assertThat(createdJob.getTargetType()).isEqualTo("TENANT");
        Assertions.assertThat(createdJob.getTargetId()).isEqualTo(orgTenant.getId());
        Assertions.assertThat(createdJob.getRecipientEmail()).contains(tenantAdmin.getEmail());
    }
}
