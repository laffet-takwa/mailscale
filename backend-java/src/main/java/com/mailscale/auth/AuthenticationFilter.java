package com.mailscale.auth;

import com.mailscale.common.OrganizationContext;
import com.mailscale.common.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the caller from either a Bearer JWT or an X-API-Key header, verifies
 * membership of the organization, and populates the tenant context.
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final ApiKeyService apiKeyService;

    public AuthenticationFilter(JwtService jwtService,
                                  UserRepository userRepository,
                                  OrganizationRepository organizationRepository,
                                  OrganizationMemberRepository memberRepository,
                                  ApiKeyService apiKeyService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            if (isPublicEndpoint(path)) {
                chain.doFilter(request, response);
                return;
            }

            String bearer = extractBearer(request);
            String apiKey = request.getHeader("X-API-Key");

            if (bearer != null) {
                authenticateWithJwt(bearer);
            } else if (apiKey != null) {
                authenticateWithApiKey(apiKey);
            }
            // else: anonymous; security config decides which endpoints allow it

            chain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateWithJwt(String bearer) {
        Claims claims;
        try {
            claims = jwtService.parse(bearer);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        JwtService.TokenClaims tokenClaims = JwtService.TokenClaims.from(claims);
        User user = userRepository.findById(tokenClaims.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User is not active");
        }
        Organization organization = organizationRepository.findById(tokenClaims.orgId())
                .orElseThrow(() -> new UnauthorizedException("Organization not found"));
        OrganizationMember member = memberRepository
                .findByOrganizationIdAndUserId(organization.getId(), user.getId())
                .orElseThrow(() -> new UnauthorizedException("Not a member of this organization"));
        if (member.getRole() != tokenClaims.role()) {
            // role changed since token issue - token is stale
            throw new UnauthorizedException("Role changed, please log in again");
        }
        OrganizationContext.set(user, organization);
        setSecurityContext(user, List.of("ROLE_" + tokenClaims.role().name()));
    }

    private void authenticateWithApiKey(String apiKey) {
        ApiKey key = apiKeyService.verify(apiKey);
        Organization organization = key.getOrganization();
        User user = userRepository.findByEmail("apikey:" + key.getId())
                .orElse(null);
        // API keys act as the organization principal with the key's scopes
        OrganizationContext.set(new User(
                        "apikey:" + key.getId(),
                        key.getKeyHash(),
                        "API Key " + key.getName()),
                organization);
        setSecurityContext(OrganizationContext.requireUser(),
                List.of("ROLE_APIKEY"));
    }

    private void setSecurityContext(User user, List<String> authorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities.stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth")
                || path.startsWith("/actuator/health")
                || path.equals("/actuator/info")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/")
                || path.startsWith("/error");
    }
}
