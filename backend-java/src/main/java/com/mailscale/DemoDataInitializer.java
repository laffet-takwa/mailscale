package com.mailscale;

import com.mailscale.auth.Organization;
import com.mailscale.auth.OrganizationMember;
import com.mailscale.auth.OrganizationMemberRepository;
import com.mailscale.auth.OrganizationRepository;
import com.mailscale.auth.Role;
import com.mailscale.auth.User;
import com.mailscale.auth.UserRepository;
import com.mailscale.contact.Contact;
import com.mailscale.contact.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a demo organization, admin user, and contacts
 * when {@code mailscale.demo.seed-enabled=true}.
 *
 * <p>Idempotent: checks for the demo user before creating
 * anything. The admin password is hashed with the
 * configured encoder; it is never logged.
 */
@Component
@ConditionalOnProperty(name = "mailscale.demo.seed-enabled",
        havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(
            DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final ContactRepository contactRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties demoProperties;

    public DemoDataInitializer(UserRepository userRepository,
                                 OrganizationRepository organizationRepository,
                                 OrganizationMemberRepository memberRepository,
                                 ContactRepository contactRepository,
                                 PasswordEncoder passwordEncoder,
                                 DemoProperties demoProperties) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.contactRepository = contactRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoProperties = demoProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(demoProperties.getAdminEmail())
                .isPresent()) {
            return;
        }
        Organization org = organizationRepository
                .findBySlug("demo")
                .orElseThrow(() -> new IllegalStateException(
                        "Demo organization missing from migration"));

        User admin = new User(demoProperties.getAdminEmail(),
                passwordEncoder.encode(demoProperties.getAdminPassword()),
                "MailScale Demo Admin");
        userRepository.save(admin);
        memberRepository.save(new OrganizationMember(
                org, admin, Role.OWNER));

        contactRepository.save(new Contact(org,
                "anna@example.com", "Anna", "Smith", null));
        contactRepository.save(new Contact(org,
                "ben@example.com", "Ben", "Jones", null));
        contactRepository.save(new Contact(org,
                "carla@example.com", "Carla", "Reyes", null));

        log.info("Demo data seeded: admin={} org={}",
                demoProperties.getAdminEmail(), org.getSlug());
    }
}
