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
                description = "API documentation for voucher, redemption, billing, and transactions."
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "1. Auth", description = "Register and login endpoints"),
                @Tag(name = "2. Users", description = "User management APIs"),
                @Tag(name = "3. Vouchers", description = "Voucher management and eligibility APIs"),
                @Tag(name = "4. Redemptions", description = "Voucher redemption and history APIs"),
                @Tag(name = "5. Bills", description = "Billing APIs"),
                @Tag(name = "6. Transactions", description = "Transaction APIs")
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
