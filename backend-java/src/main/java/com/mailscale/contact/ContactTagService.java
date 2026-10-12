package com.mailscale.contact;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTagService {

    private final ContactTagRepository contactTagRepository;

    public ContactTagService(ContactTagRepository contactTagRepository) {
        this.contactTagRepository = contactTagRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactTag> list(Organization org) {
        return contactTagRepository.findByOrganizationId(org.getId());
    }

    @Transactional
    public ContactTag create(Organization org, String name) {
        if (contactTagRepository.findByOrganizationIdAndName(org.getId(), name)
                .isPresent()) {
            throw new ConflictException("Tag already exists");
        }
        return contactTagRepository.save(new ContactTag(org, name.trim()));
    }

    @Transactional
    public void delete(Organization org, Long id) {
        ContactTag tag = contactTagRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Tag not found"));
        contactTagRepository.delete(tag);
    }
}
