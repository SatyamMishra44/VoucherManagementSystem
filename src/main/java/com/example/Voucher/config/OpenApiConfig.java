package com.example.Voucher.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Voucher Management System API",
                version = "v1",
                description = "Simple flow: create account, login, admin creates voucher templates, users buy vouchers, users redeem balance, and history is available for audit."
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "1. Auth", description = "Step 1-2: Sign up and login to get your access token."),
                @Tag(name = "2. Users", description = "Admin-only user list."),
                @Tag(name = "3. Admin Vouchers", description = "Step 3: Admin creates and enables/disables voucher templates."),
                @Tag(name = "4. User Vouchers", description = "Step 4-7: User sees templates, purchases vouchers, redeems balance, and checks redemption history."),
                @Tag(name = "5. Bills", description = "Optional billing records used for redemption and reporting."),
                @Tag(name = "6. Transactions", description = "Payment/settlement records created during voucher redemption with bills.")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
    // Central place for OpenAPI metadata and security/header docs.

    private static final String TENANT_HEADER = "X-Tenant-Code";

    @Bean
    public OpenApiCustomizer tenantHeaderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(this::addTenantHeaderIfMissing)
            );
        };
    }

    private void addTenantHeaderIfMissing(Operation operation) {
        if (operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(p -> TENANT_HEADER.equalsIgnoreCase(p.getName()) && "header".equalsIgnoreCase(p.getIn()))) {
            return;
        }

        Parameter tenantHeaderParam = new Parameter()
                .in("header")
                .name(TENANT_HEADER)
                .required(false)
                .description("Tenant code for multi-tenant routing. Example: 123, ACME. Defaults to SYSTEM_INDIVIDUAL when omitted.")
                .schema(new StringSchema());

        operation.addParametersItem(tenantHeaderParam);
    }
}
