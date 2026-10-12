package com.mailscale.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("select o from OutboxEvent o where o.publishedAt is null order by o.id")
    List<OutboxEvent> findUnpublished();

    @Modifying
    @Query("update OutboxEvent o set o.lockedAt = :now where o.id = :id and o.publishedAt is null"
            + " and o.lockedAt is null")
    int claim(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("update OutboxEvent o set o.publishedAt = :now where o.id = :id")
    int markPublished(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("update OutboxEvent o set o.lockedAt = null where o.id = :id")
    int releaseLock(@Param("id") Long id);

    @Modifying
    @Query("delete from OutboxEvent o where o.publishedAt is not null"
            + " and o.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
