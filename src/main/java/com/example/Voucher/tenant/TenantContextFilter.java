package com.example.Voucher.tenant;

import com.example.Voucher.security.TenantAwareUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantResolverService tenantResolverService;

    public TenantContextFilter(TenantResolverService tenantResolverService) {
        this.tenantResolverService = tenantResolverService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
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

            TenantContext.setTenantId(resolvedTenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long resolveHeaderTenantId(HttpServletRequest request) {
        String tenantCodeHeader = request.getHeader(TenantConstants.TENANT_HEADER);
        if (!StringUtils.hasText(tenantCodeHeader)) {
            return null;
        }
        return tenantResolverService.resolveTenantId(tenantCodeHeader);
    }

    private Long resolveAuthenticatedTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof TenantAwareUserDetails tenantAwareUserDetails) {
            return tenantAwareUserDetails.getTenantId();
        }
        return null;
    }
}
