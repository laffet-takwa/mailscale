package com.mailscale.auth;

import com.mailscale.common.ConflictException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         OrganizationMemberRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with this email already exists");
        }
        String slug = request.organizationSlug() != null && !request.organizationSlug().isBlank()
                ? request.organizationSlug().trim().toLowerCase()
                : email.split("@")[0] + "-org";
        slug = slug.replaceAll("[^a-z0-9-]", "-");
        if (organizationRepository.existsBySlug(slug)) {
            throw new ConflictException("Organization slug already taken");
        }

        User user = new User(email,
                passwordEncoder.encode(request.password()),
                request.fullName());
        userRepository.save(user);

        Organization organization = new Organization(
                request.organizationName() != null ? request.organizationName() : slug,
                slug);
        organizationRepository.save(organization);

        memberRepository.save(new OrganizationMember(organization, user, Role.OWNER));

        String token = jwtService.createToken(user, organization, Role.OWNER);
        return new RegisterResponse(token, user.getId(), organization.getId(),
                organization.getSlug(), Role.OWNER.name());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.mailscale.common.UnauthorizedException(
                        "Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new com.mailscale.common.UnauthorizedException("User is suspended");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new com.mailscale.common.UnauthorizedException(
                    "Invalid email or password");
        }
        OrganizationMember member = memberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new com.mailscale.common.UnauthorizedException(
                        "No organization membership"));
        String token = jwtService.createToken(user, member.getOrganization(),
                member.getRole());
        return new LoginResponse(token, user.getId(),
                member.getOrganization().getId(),
                member.getOrganization().getSlug(), member.getRole().name());
    }

    public record RegisterRequest(String email, String password, String fullName,
                                     String organizationName, String organizationSlug) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record RegisterResponse(String token, Long userId, Long organizationId,
                                     String organizationSlug, String role) {
    }

    public record LoginResponse(String token, Long userId, Long organizationId,
                                   String organizationSlug, String role) {
    }
}
