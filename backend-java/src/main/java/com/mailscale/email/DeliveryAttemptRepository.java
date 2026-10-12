package com.mailscale.email;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findByMessageIdOrderByCreatedAtDesc(java.util.UUID messageId);
}
