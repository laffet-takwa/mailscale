package com.mailscale.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ContactTest {

    @Test
    void unsubscribeMarksStatusAndTimestamp() {
        Contact contact = new Contact(null,
                "a@example.com", "A", "B", null);
        assertTrue(contact.isDeliverable());
        contact.unsubscribe(Instant.now());
        assertEquals(ContactStatus.UNSUBSCRIBED,
                contact.getStatus());
        assertFalse(contact.isDeliverable());
    }

    @Test
    void resubscribeRestoresDeliverability() {
        Contact contact = new Contact(null,
                "a@example.com", "A", "B", null);
        contact.unsubscribe(Instant.now());
        contact.resubscribe(Instant.now());
        assertEquals(ContactStatus.ACTIVE, contact.getStatus());
        assertTrue(contact.isDeliverable());
    }

    @Test
    void bouncedContactsAreNotDeliverable() {
        Contact contact = new Contact(null,
                "a@example.com", "A", "B", null);
        contact.markBounced();
        assertFalse(contact.isDeliverable());
    }
}
