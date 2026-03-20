package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthLoginRequestDto;
import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected VoucherTemplateRepository voucherTemplateRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected RoleProperties roleProperties;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM tenant_voucher_distributions");
        jdbcTemplate.execute("DELETE FROM tenant_voucher_inventory");
        jdbcTemplate.execute("DELETE FROM redemption_history");
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM bills");
        jdbcTemplate.execute("DELETE FROM user_vouchers");
        jdbcTemplate.execute("DELETE FROM report_jobs");
        jdbcTemplate.execute("DELETE FROM tenant_audit_logs");
        jdbcTemplate.execute("DELETE FROM tenant_voucher_requests");
        jdbcTemplate.execute("DELETE FROM tenant_onboarding_requests");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM voucher_templates");
        jdbcTemplate.execute("DELETE FROM tenants WHERE tenant_code <> 'SYSTEM_INDIVIDUAL'");
    }

    protected Tenant systemTenant() {
        return tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE)
                .orElseThrow(() -> new IllegalStateException("SYSTEM_INDIVIDUAL tenant missing"));
    }

    protected Tenant createOrganizationTenant(String tenantCode) {
        return tenantRepository.findByTenantCodeAndActiveTrue(tenantCode)
                .orElseGet(() -> tenantRepository.save(new Tenant(tenantCode, tenantCode + " Org", TenantType.ORGANIZATION)));
    }

    protected VoucherTemplate createVoucherTemplate(String code, Tenant tenant) {
        VoucherTemplate template = new VoucherTemplate(
                code,
                new BigDecimal("10.00"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );
        template.setTenantId(tenant.getId());
        return voucherTemplateRepository.save(template);
    }

    protected Role roleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Missing role: " + name));
    }

    protected User createUser(String firstName, String lastName, String email, String phone,
                              String rawPassword, Role role, Tenant tenant) {
        User user = new User(
                firstName,
                lastName,
                passwordEncoder.encode(rawPassword),
                phone,
                email,
                LocalDateTime.now()
        );
        user.setTenantId(tenant.getId());
        user.addRole(role);
        return userRepository.save(user);
    }

    protected void registerUser(AuthRegisterRequestDto request, String tenantCode) {
        HttpHeaders headers = jsonHeaders(tenantCode);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Void.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    protected AuthResponseDto login(String email, String password, String tenantCode) {
        AuthLoginRequestDto request = new AuthLoginRequestDto();
        request.setEmail(email);
        request.setPassword(password);

        HttpHeaders headers = jsonHeaders(tenantCode);
        ResponseEntity<AuthResponseDto> response = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                AuthResponseDto.class
        );
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    protected HttpHeaders authHeaders(String accessToken, String tenantCode) {
        HttpHeaders headers = jsonHeaders(tenantCode);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }

    protected String uniquePhone() {
        long value = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return Long.toString(value);
    }

    protected String uniqueTenantCode(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + "_" + suffix;
    }

    protected <T> T readBody(ResponseEntity<String> response, Class<T> type) {
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Response body is empty");
        }
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse response body: " + body, ex);
        }
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
