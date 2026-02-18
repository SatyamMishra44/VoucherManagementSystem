package com.example.Voucher.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    // Central place for OpenAPI metadata and JWT security scheme.
}
