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
 * Resolves the caller from either a Bearer JWT or an X-API-Key header,
 * verifies membership of the organization, and populates the tenant
 * context.
 *
 * <p>Authentication failures are written directly as a 401 JSON
 * response so no stack trace or Spring-specific payload leaks. The
 * filter never trusts a tenant identifier supplied by the client: the
 * organization always comes from the verified credential.
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            AuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final ApiKeyService apiKeyService;
    private final UnauthorizedHandler unauthorizedHandler;

    public AuthenticationFilter(JwtService jwtService,
                                  UserRepository userRepository,
                                  OrganizationRepository organizationRepository,
                                  OrganizationMemberRepository memberRepository,
                                  ApiKeyService apiKeyService,
                                  UnauthorizedHandler unauthorizedHandler) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.apiKeyService = apiKeyService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException,
            IOException {
        try {
            String path = request.getRequestURI();
            if (isPublicEndpoint(path)) {
                chain.doFilter(request, response);
                return;
            }

            String bearer = extractBearer(request);
            String apiKey = request.getHeader("X-API-Key");

            if (bearer != null) {
                if (!authenticateWithJwt(bearer, request, response)) {
                    return;
                }
            } else if (apiKey != null) {
                if (!authenticateWithApiKey(apiKey, request, response)) {
                    return;
                }
            }
            // else: anonymous; security config decides which routes allow it

            chain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** @return true when the request should continue down the chain */
    private boolean authenticateWithJwt(String bearer,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
            throws IOException {
        Claims claims;
        try {
            claims = jwtService.parse(bearer);
        } catch (Exception e) {
            unauthorizedHandler.handle(request, response,
                    "Invalid or expired token");
            return false;
        }
        try {
            JwtService.TokenClaims tokenClaims =
                    JwtService.TokenClaims.from(claims);
            User user = userRepository.findById(tokenClaims.userId())
                    .orElseThrow(() -> new UnauthorizedException(
                            "User not found"));
            if (user.getStatus() != UserStatus.ACTIVE) {
                unauthorizedHandler.handle(request, response,
                        "User is not active");
                return false;
            }
            Organization organization = organizationRepository
                    .findById(tokenClaims.orgId())
                    .orElseThrow(() -> new UnauthorizedException(
                            "Organization not found"));
            OrganizationMember member = memberRepository
                    .findByOrganizationIdAndUserId(organization.getId(),
                            user.getId())
                    .orElseThrow(() -> new UnauthorizedException(
                            "Not a member of this organization"));
            if (member.getRole() != tokenClaims.role()) {
                unauthorizedHandler.handle(request, response,
                        "Role changed, please log in again");
                return false;
            }
            OrganizationContext.set(user, organization);
            setSecurityContext(user,
                    List.of("ROLE_" + tokenClaims.role().name()));
            return true;
        } catch (UnauthorizedException e) {
            unauthorizedHandler.handle(request, response, e.getMessage());
            return false;
        }
    }

    /** @return true when the request should continue down the chain */
    private boolean authenticateWithApiKey(String apiKey,
                                             HttpServletRequest request,
                                             HttpServletResponse response)
            throws IOException {
        try {
            ApiKey key = apiKeyService.verify(apiKey);
            Organization organization = key.getOrganization();
            User principal = new User(
                    "apikey:" + key.getId(),
                    key.getKeyHash(),
                    "API Key " + key.getName());
            OrganizationContext.set(principal, organization);
            setSecurityContext(principal,
                    List.of("ROLE_APIKEY"));
            return true;
        } catch (UnauthorizedException e) {
            unauthorizedHandler.handle(request, response, e.getMessage());
            return false;
        }
    }

    private void setSecurityContext(User user, List<String> authorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities.stream()
                        .map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Endpoints reachable without a credential. Only the registration
     * and login routes are public; every other /api/v1 route requires
     * authentication, including /auth/me and /auth/api-keys.
     */
    private boolean isPublicEndpoint(String path) {
        return path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.startsWith("/actuator/health")
                || path.equals("/actuator/info")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/")
                || path.startsWith("/error");
    }
}
