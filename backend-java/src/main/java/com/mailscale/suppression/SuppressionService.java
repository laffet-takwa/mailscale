package com.mailscale.suppression;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuppressionService {

    private final SuppressionEntryRepository suppressionEntryRepository;

    public SuppressionService(
            SuppressionEntryRepository suppressionEntryRepository) {
        this.suppressionEntryRepository = suppressionEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<SuppressionEntry> list(Organization org) {
        return suppressionEntryRepository.findByOrganizationId(org.getId());
    }

    @Transactional
    public SuppressionEntry add(Organization org, String email,
                                  SuppressionReason reason) {
        if (suppressionEntryRepository
                .existsByOrganizationIdAndEmail(org.getId(), email)) {
            throw new ConflictException("Email is already suppressed");
        }
        return suppressionEntryRepository.save(
                new SuppressionEntry(org, email, reason));
    }

    @Transactional
    public void remove(Organization org, Long id) {
        SuppressionEntry entry = suppressionEntryRepository
                .findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new NotFoundException("Suppression entry not found"));
        suppressionEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public boolean isSuppressed(Organization org, String email) {
        return suppressionEntryRepository
                .existsByOrganizationIdAndEmail(org.getId(), email);
    }
}
