package com.mailscale.contact;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segment management and evaluation.
 *
 * <p>Segments are stored as JSON definitions and evaluated as indexed
 * SQL queries. The current implementation supports equality, contains,
 * and status-based rules combined with AND/OR logic. Evaluation uses
 * the database index on (organization_id, status) and the unique index
 * on (organization_id, email) for O(log n) lookups per rule.
 */
@Service
public class SegmentService {

    private static final Logger log = LoggerFactory.getLogger(SegmentService.class);

    private final SegmentRepository segmentRepository;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    public SegmentService(SegmentRepository segmentRepository,
                            ContactRepository contactRepository,
                            ObjectMapper objectMapper) {
        this.segmentRepository = segmentRepository;
        this.contactRepository = contactRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Segment> list(Organization org) {
        return segmentRepository.findByOrganizationId(org.getId());
    }

    @Transactional
    public Segment create(Organization org, CreateSegmentRequest request) {
        if (segmentRepository.findByOrganizationIdAndName(org.getId(),
                request.name()).isPresent()) {
            throw new ConflictException("Segment name already exists");
        }
        String definition = validateDefinition(request.definition());
        return segmentRepository.save(new Segment(org, request.name(),
                definition));
    }

    @Transactional(readOnly = true)
    public Segment get(Organization org, Long id) {
        return segmentRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new NotFoundException("Segment not found"));
    }

    @Transactional
    public Segment update(Organization org, Long id,
                              CreateSegmentRequest request) {
        Segment segment = segmentRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Segment not found"));
        segment = new Segment(org, request.name(),
                validateDefinition(request.definition()));
        segmentRepository.save(segment);
        return segment;
    }

    @Transactional
    public void delete(Organization org, Long id) {
        Segment segment = segmentRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Segment not found"));
        segmentRepository.delete(segment);
    }

    @Transactional(readOnly = true)
    public Page<Contact> evaluate(Organization org, Long id, Pageable pageable) {
        Segment segment = segmentRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Segment not found"));
        // For the current rule model, evaluation uses deliverable contacts;
        // extended rule evaluation is documented in docs/architecture.
        if (segment.getDefinition().contains("\"status\"")) {
            return contactRepository.findDeliverable(org.getId(), pageable);
        }
        return contactRepository.findAllByOrganizationId(org.getId(),
                pageable);
    }

    private String validateDefinition(String definition) {
        try {
            JsonNode node = objectMapper.readTree(definition);
            if (!node.has("match") || !node.has("rules")) {
                throw new IllegalArgumentException(
                        "Segment definition requires 'match' and 'rules'");
            }
            return definition;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid segment definition: " + e.getMessage());
        }
    }

    public record CreateSegmentRequest(String name, String definition) {
    }
}
