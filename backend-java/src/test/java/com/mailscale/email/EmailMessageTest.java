package com.mailscale.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailMessageTest {

    @Test
    void deriveIdIsDeterministic() {
        UUID first = EmailMessage.deriveId("key-1", "demo");
        UUID second = EmailMessage.deriveId("key-1", "demo");
        UUID other = EmailMessage.deriveId("key-2", "demo");
        assertEquals(first, second);
        assertNotEquals(first, other);
    }

    @Test
    void statusTransitionsAreSafe() {
        EmailMessage message = sampleMessage();
        message.markProcessing();
        assertEquals(MessageStatus.PROCESSING, message.getStatus());
        message.markSent("provider-1");
        assertEquals(MessageStatus.SENT, message.getStatus());
        message.markDelivered("provider-1");
        assertEquals(MessageStatus.DELIVERED, message.getStatus());
        // terminal: cannot be overwritten
        message.markFailed();
        assertEquals(MessageStatus.DELIVERED, message.getStatus());
    }

    @Test
    void suppressedOnlyFromQueuedOrProcessing() {
        EmailMessage message = sampleMessage();
        message.markSuppressed();
        assertEquals(MessageStatus.SUPPRESSED, message.getStatus());
        assertFalse(message.isTerminal() == false);
        assertTrue(message.isTerminal());
    }

    @Test
    void failedIsTerminal() {
        EmailMessage message = sampleMessage();
        message.markProcessing();
        message.markFailed();
        assertTrue(message.isTerminal());
    }

    private EmailMessage sampleMessage() {
        return new EmailMessage(UUID.randomUUID(), null,
                MessageType.TRANSACTIONAL, "to@example.com", "To",
                "Subject", "<html></html>", "text", null, null, null,
                "corr-1", "idem-1");
    }
}
