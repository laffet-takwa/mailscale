package com.mailscale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailscale.auth.Organization;
import com.mailscale.auth.OrganizationRepository;
import com.mailscale.contact.Contact;
import com.mailscale.contact.ContactRepository;
import com.mailscale.contact.ContactService;
import com.mailscale.contact.ContactService.ImportResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContactServiceIntegrationTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization org;

    @BeforeEach
    void setUp() {
        org = organizationRepository.findBySlug("demo")
                .orElseThrow();
    }

    @Test
    void createAndFindContact() {
        Contact created = contactService.create(org,
                new ContactService.CreateContactRequest(
                        "test@example.com", "Test", "User", null));
        assertNotNull(created.getId());

        Contact found = contactService.get(org, created.getId());
        assertEquals("test@example.com", found.getEmail());
        assertEquals("Test", found.getFirstName());
    }

    @Test
    void duplicateEmailIsRejected() {
        contactService.create(org,
                new ContactService.CreateContactRequest(
                        "dup@example.com", "A", "B", null));
        assertThrows(com.mailscale.common.ConflictException.class,
                () -> contactService.create(org,
                        new ContactService.CreateContactRequest(
                                "dup@example.com", "A", "B", null)));
    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> contactService.create(org,
                        new ContactService.CreateContactRequest(
                                "not-an-email", "A", "B", null)));
    }

    @Test
    void csvImportProcessesRowsAndReportsIssues() {
        String csv = "email,firstName,lastName\n"
                + "one@example.com,One,User\n"
                + "two@example.com,Two,User\n"
                + "bad-email,Bad,User\n"
                + "one@example.com,One,Dup\n";
        ImportResult result = contactService.importCsv(org, csv);

        assertEquals(2, result.processed());
        assertEquals(1, result.duplicates().size());
        assertEquals(1, result.errors().size());
        assertTrue(contactRepository
                .findByOrganizationIdAndEmail(org.getId(),
                        "one@example.com").isPresent());
        assertTrue(contactRepository
                .findByOrganizationIdAndEmail(org.getId(),
                        "two@example.com").isPresent());
    }

    @Test
    void unsubscribeSuppressesContact() {
        Contact contact = contactService.create(org,
                new ContactService.CreateContactRequest(
                        "unsub@example.com", "Un", "Sub", null));
        Contact unsubscribed = contactService.unsubscribe(org,
                contact.getId(), java.time.Instant.now());
        assertEquals("UNSUBSCRIBED",
                unsubscribed.getStatus().name());
    }

    @Test
    void listIsPaginated() {
        for (int i = 0; i < 5; i++) {
            contactService.create(org,
                    new ContactService.CreateContactRequest(
                            "page" + i + "@example.com",
                            "P" + i, "User", null));
        }
        var page = contactService.list(org,
                PageRequest.of(0, 3));
        assertEquals(3, page.getContent().size());
        assertTrue(page.getTotalElements() >= 5);
    }
}
