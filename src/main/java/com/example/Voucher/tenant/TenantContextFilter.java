package com.example.Voucher.tenant;

import com.example.Voucher.security.TenantAwareUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;



// this filter run only once per request
@Component
public class TenantContextFilter extends OncePerRequestFilter { //

    private final TenantResolverService tenantResolverService;

    public TenantContextFilter(TenantResolverService tenantResolverService) {
        this.tenantResolverService = tenantResolverService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        /* skip tenant logic for swagger APIs because these endpoints don't need tenant context
        */

        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Long headerTenantId = resolveHeaderTenantId(request);
            Long authenticatedTenantId = resolveAuthenticatedTenantId();
            Long resolvedTenantId;

            if (authenticatedTenantId != null) {
                resolvedTenantId = tenantResolverService.requireActiveTenantId(authenticatedTenantId);
                if (headerTenantId != null && !headerTenantId.equals(resolvedTenantId)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant mismatch");
                    return;
                }
            } else if (headerTenantId != null) {
                resolvedTenantId = headerTenantId;
            } else {
                resolvedTenantId = tenantResolverService.resolveTenantId(null);
            }

            TenantContext.setTenantId(resolvedTenantId); // add tenantId into tenantContext
            if (resolvedTenantId != null) {
                MDC.put("tenantId", String.valueOf(resolvedTenantId));// mdc is the logging context from slf4j.
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }



    // Extracts tenant info from HTTP request header
    private Long resolveHeaderTenantId(HttpServletRequest request) {
        String tenantCodeHeader = request.getHeader(TenantConstants.TENANT_HEADER);
        if (!StringUtils.hasText(tenantCodeHeader)) {// validate the header checks,not null,not spaces
            return null;
        }
        return tenantResolverService.resolveTenantId(tenantCodeHeader);
    }


    // extract tenants form logged-in users from spring Security
    private Long resolveAuthenticatedTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal(); // principal is actual user object this will return the actual logged-in user
        if (principal instanceof TenantAwareUserDetails tenantAwareUserDetails) { // validates from the tenantAwareUserDetails
            return tenantAwareUserDetails.getTenantId();
        }
        return null;
    }
}
