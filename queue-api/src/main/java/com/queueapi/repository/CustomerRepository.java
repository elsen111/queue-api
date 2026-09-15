package com.queueapi.repository;

import com.queueapi.entity.CustomerEntity;
import com.queueapi.enums.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Page<CustomerEntity> findAllByStatusOrderByCreatedAtAsc(QueueStatus status, Pageable pageable);

    Page<CustomerEntity> findAllByStatus(QueueStatus status, Pageable pageable);

    long countByStatusAndCreatedAtLessThan(QueueStatus status, Instant createdAt);

    long countByStatus(QueueStatus status);

    Optional<CustomerEntity> findFirstByStatusOrderByCreatedAtAsc(QueueStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerEntity c where c.status = :status order by c.createdAt asc")
    List<CustomerEntity> lockNextWaiting(QueueStatus status);
}