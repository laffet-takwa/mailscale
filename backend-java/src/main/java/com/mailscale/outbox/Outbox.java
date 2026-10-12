package com.mailscale.outbox;

import com.mailscale.outbox.OutboxEvent;
import com.mailscale.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service that records domain events into the transactional
 * outbox. Callers persist their aggregate and the event in the same
 * transaction, guaranteeing no lost events on crash (ADR-0003).
 */
@Service
public class Outbox {

    private final OutboxEventRepository outboxEventRepository;

    public Outbox(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEvent record(String aggregateType, String aggregateId,
                                String eventType, String payload) {
        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId,
                eventType, payload);
        return outboxEventRepository.save(event);
    }
}
