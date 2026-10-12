package com.mailscale.contact;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import com.mailscale.suppression.SuppressionReason;
import com.mailscale.suppression.SuppressionEntry;
import com.mailscale.suppression.SuppressionEntryRepository;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contact management: CRUD, CSV import with validation, duplicate
 * detection, consent handling, and suppression enforcement.
 *
 * <p>CSV import uses a hash set for duplicate detection within the file:
 * O(n) time and O(n) memory for n rows, capped by MAX_IMPORT_ROWS.
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    static final int MAX_IMPORT_ROWS = 10_000;
    static final int MAX_CSV_BYTES = 5 * 1024 * 1024; // 5 MB

    private final ContactRepository contactRepository;
    private final ContactTagRepository contactTagRepository;
    private final SuppressionEntryRepository suppressionEntryRepository;

    public ContactService(ContactRepository contactRepository,
                            ContactTagRepository contactTagRepository,
                            SuppressionEntryRepository suppressionEntryRepository) {
        this.contactRepository = contactRepository;
        this.contactTagRepository = contactTagRepository;
        this.suppressionEntryRepository = suppressionEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<Contact> list(Organization org, Pageable pageable) {
        return contactRepository.findAllByOrganizationId(org.getId(),
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<Contact> search(Organization org, String query,
                                      Pageable pageable) {
        return contactRepository.search(org.getId(), query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Contact> findByStatus(Organization org,
                                            ContactStatus status,
                                            Pageable pageable) {
        return contactRepository.findByStatus(org.getId(), status,
                pageable);
    }

    @Transactional
    public Contact create(Organization org, CreateContactRequest request) {
        String email = request.email().trim().toLowerCase();
        validateEmail(email);
        if (contactRepository.findByOrganizationIdAndEmail(org.getId(), email)
                .isPresent()) {
            throw new ConflictException("Contact already exists");
        }
        Contact contact = new Contact(org, email, request.firstName(),
                request.lastName(), request.attributes());
        return contactRepository.save(contact);
    }

    @Transactional(readOnly = true)
    public Contact get(Organization org, Long id) {
        return contactRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
    }

    @Transactional
    public Contact update(Organization org, Long id,
                              UpdateContactRequest request) {
        Contact contact = contactRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
        if (request.firstName() != null) {
            contact.update(request.firstName(), request.lastName(),
                    request.attributes());
        }
        return contactRepository.save(contact);
    }

    @Transactional
    public void delete(Organization org, Long id) {
        Contact contact = contactRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
        contactRepository.delete(contact);
    }

    @Transactional
    public Contact unsubscribe(Organization org, Long id, Instant now) {
        Contact contact = contactRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
        contact.unsubscribe(now);
        contactRepository.save(contact);
        suppress(org, contact.getEmail(), SuppressionReason.UNSUBSCRIBE);
        return contact;
    }

    @Transactional
    public Contact resubscribe(Organization org, Long id, Instant now) {
        Contact contact = contactRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
        contact.resubscribe(now);
        contactRepository.save(contact);
        suppressionEntryRepository
                .findByOrganizationIdAndEmail(org.getId(), contact.getEmail())
                .ifPresent(s -> suppressionEntryRepository.delete(s));
        return contact;
    }

    public void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    @Transactional
    public ImportResult importCsv(Organization org, String csvContent) {
        if (csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                > MAX_CSV_BYTES) {
            throw new IllegalArgumentException("CSV file exceeds 5 MB limit");
        }
        ImportResult result = new ImportResult();
        Set<String> seenInFile = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent));
                CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                        .setHeader("email", "firstName", "lastName")
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build()
                        .parse(reader)) {
            for (CSVRecord record : parser) {
                if (result.processed() >= MAX_IMPORT_ROWS) {
                    result.addError(0, "Row limit of " + MAX_IMPORT_ROWS
                            + " exceeded, remaining rows skipped");
                    break;
                }
                int row = result.processed() + 1;
                String email = record.isSet("email")
                        ? record.get("email").trim().toLowerCase() : "";
                try {
                    if (email.isBlank()) {
                        throw new IllegalArgumentException("Email is required");
                    }
                    validateEmail(email);
                    if (!seenInFile.add(email)) {
                        result.addDuplicate(email);
                        continue;
                    }
                    if (contactRepository
                            .findByOrganizationIdAndEmail(org.getId(), email)
                            .isPresent()) {
                        result.addDuplicate(email);
                        continue;
                    }
                    Contact contact = new Contact(org, email,
                            record.isSet("firstName") ? record.get("firstName") : null,
                            record.isSet("lastName") ? record.get("lastName") : null,
                            null);
                    contactRepository.save(contact);
                    result.incrementProcessed();
                } catch (IllegalArgumentException e) {
                    result.addError(row, email + ": " + e.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Failed to parse CSV", e);
        }
        log.info("CSV import org={} processed={} duplicates={} errors={}",
                org.getSlug(), result.processed(), result.duplicates().size(),
                result.errors().size());
        return result;
    }

    @Transactional
    public void suppress(Organization org, String email,
                             SuppressionReason reason) {
        if (suppressionEntryRepository
                .existsByOrganizationIdAndEmail(org.getId(), email)) {
            return;
        }
        suppressionEntryRepository.save(
                new SuppressionEntry(org, email, reason));
    }

    public record CreateContactRequest(String email, String firstName,
                                         String lastName, String attributes) {
    }

    public record UpdateContactRequest(String firstName, String lastName,
                                         String attributes) {
    }

    public record ImportError(int row, String message) {
    }

    /**
     * Mutable accumulation of a CSV import, exposed as an
     * immutable snapshot via accessor methods.
     */
    public static class ImportResult {

        private final List<String> duplicates = new ArrayList<>();
        private final List<ImportError> errors = new ArrayList<>();
        private int processedCount;

        public void incrementProcessed() {
            processedCount++;
        }

        public void addDuplicate(String email) {
            duplicates.add(email);
        }

        public void addError(int row, String message) {
            errors.add(new ImportError(row, message));
        }

        public int processed() {
            return processedCount;
        }

        public List<String> duplicates() {
            return List.copyOf(duplicates);
        }

        public List<ImportError> errors() {
            return List.copyOf(errors);
        }
    }
}
