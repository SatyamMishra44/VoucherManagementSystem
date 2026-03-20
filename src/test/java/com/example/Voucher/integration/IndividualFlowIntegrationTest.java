package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.VoucherTemplateResponseDto;
import com.example.Voucher.tenant.Tenant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IndividualFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void individualUser_canRegisterLoginAndViewEligibleTemplates() {
        Tenant systemTenant = systemTenant();
        String templateCode = "IND-" + System.currentTimeMillis();
        createVoucherTemplate(templateCode, systemTenant);

        String email = uniqueEmail("individual");
        String password = "Password123!";

        AuthRegisterRequestDto register = new AuthRegisterRequestDto();
        register.setFirstName("Ind");
        register.setLastName("User");
        register.setEmail(email);
        register.setPhoneNumber(uniquePhone());
        register.setPassword(password);

        registerUser(register, null);

        AuthResponseDto auth = login(email, password, null);

        ResponseEntity<List<VoucherTemplateResponseDto>> response = restTemplate.exchange(
                "/api/v1/vouchers",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<VoucherTemplateResponseDto>>() {
                }
        );

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody())
                .extracting(VoucherTemplateResponseDto::getCode)
                .contains(templateCode);
    }
}
