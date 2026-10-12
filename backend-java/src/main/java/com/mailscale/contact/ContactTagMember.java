package com.mailscale.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "contact_tag_members")
@EntityListeners(AuditingEntityListener.class)
public class ContactTagMember {

    @EmbeddedId
    private ContactTagMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contactId")
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private ContactTag tag;

    protected ContactTagMember() {
    }

    public ContactTagMember(Contact contact, ContactTag tag) {
        this.contact = contact;
        this.tag = tag;
        this.id = new ContactTagMemberId(contact.getId(), tag.getId());
    }

    public ContactTagMemberId getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    public ContactTag getTag() {
        return tag;
    }

    @Embeddable
    public static class ContactTagMemberId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "contact_id")
        private Long contactId;

        @Column(name = "tag_id")
        private Long tagId;

        public ContactTagMemberId() {
        }

        public ContactTagMemberId(Long contactId, Long tagId) {
            this.contactId = contactId;
            this.tagId = tagId;
        }

        public Long getContactId() {
            return contactId;
        }

        public Long getTagId() {
            return tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContactTagMemberId other)) {
                return false;
            }
            return Objects.equals(contactId, other.contactId)
                    && Objects.equals(tagId, other.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contactId, tagId);
        }
    }
}
