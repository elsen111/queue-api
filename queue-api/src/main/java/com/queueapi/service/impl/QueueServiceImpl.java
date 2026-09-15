package com.queueapi.service.impl;

import com.queueapi.dto.request.CustomerCreateRequest;
import com.queueapi.dto.response.CustomerResponse;
import com.queueapi.dto.response.QueueStatsResponse;
import com.queueapi.entity.CustomerEntity;
import com.queueapi.enums.QueueStatus;
import com.queueapi.exception.BadRequestException;
import com.queueapi.exception.ConflictException;
import com.queueapi.exception.ResourceNotFoundException;
import com.queueapi.mapper.CustomerMapper;
import com.queueapi.repository.CustomerRepository;
import com.queueapi.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponse addCustomer(CustomerCreateRequest request) {
        CustomerEntity entity = CustomerEntity.builder()
                .name(request.getName().trim())
                .phone(request.getPhone())
                .status(QueueStatus.WAITING)
                .build();
        CustomerEntity saved = customerRepository.save(entity);
        return withPosition(saved);
    }

    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("name", "phone", "status", "createdAt", "updatedAt", "calledAt", "completedAt");

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getWaitingCustomers(Pageable pageable) {
        Page<CustomerEntity> page = customerRepository.findAllByStatusOrderByCreatedAtAsc(
                QueueStatus.WAITING, sanitizeSort(pageable));
        return page.map(this::withPosition);
    }

    private Pageable sanitizeSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<String> invalid = pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !SORTABLE_PROPERTIES.contains(property))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new BadRequestException(
                    "Cannot sort by: " + invalid + ". Allowed sort properties: " + SORTABLE_PROPERTIES);
        }
        return pageable;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        CustomerEntity entity = findOrThrow(id);
        return withPosition(entity);
    }

    @Override
    @Transactional
    public CustomerResponse callNext() {
        List<CustomerEntity> locked = customerRepository.lockNextWaiting(QueueStatus.WAITING);
        if (locked.isEmpty()) {
            throw new ConflictException("Queue is empty - no waiting customers to call");
        }
        CustomerEntity next = locked.get(0);
        next.setStatus(QueueStatus.SERVING);
        next.setCalledAt(Instant.now());
        CustomerEntity saved = customerRepository.save(next);
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeCustomer(UUID id) {
        CustomerEntity entity = findOrThrow(id);
        customerRepository.delete(entity);
    }

    @Override
    @Transactional
    public CustomerResponse completeCurrent(UUID id) {
        CustomerEntity entity = findOrThrow(id);
        if (entity.getStatus() != QueueStatus.SERVING) {
            throw new ConflictException("Customer is not currently being served");
        }
        entity.setStatus(QueueStatus.COMPLETED);
        entity.setCompletedAt(Instant.now());
        return customerMapper.toResponse(customerRepository.save(entity));
    }

    @Override
    @Transactional
    public CustomerResponse cancel(UUID id) {
        CustomerEntity entity = findOrThrow(id);
        if (entity.getStatus() == QueueStatus.COMPLETED || entity.getStatus() == QueueStatus.CANCELLED) {
            throw new ConflictException("Customer already finished (" + entity.getStatus() + ")");
        }
        entity.setStatus(QueueStatus.CANCELLED);
        return customerMapper.toResponse(customerRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatsResponse getStats() {
        CustomerResponse serving = customerRepository.findFirstByStatusOrderByCreatedAtAsc(QueueStatus.SERVING)
                .map(customerMapper::toResponse)
                .orElse(null);
        return QueueStatsResponse.builder()
                .totalWaiting(customerRepository.countByStatus(QueueStatus.WAITING))
                .totalServing(customerRepository.countByStatus(QueueStatus.SERVING))
                .totalCompleted(customerRepository.countByStatus(QueueStatus.COMPLETED))
                .totalCancelled(customerRepository.countByStatus(QueueStatus.CANCELLED))
                .currentlyServing(serving)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getByStatus(QueueStatus status, Pageable pageable) {
        return customerRepository.findAllByStatus(status, sanitizeSort(pageable)).map(customerMapper::toResponse);
    }

    private CustomerEntity findOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private CustomerResponse withPosition(CustomerEntity entity) {
        CustomerResponse response = customerMapper.toResponse(entity);
        if (entity.getStatus() == QueueStatus.WAITING) {
            long ahead = customerRepository.countByStatusAndCreatedAtLessThan(QueueStatus.WAITING, entity.getCreatedAt());
            response.setPosition(ahead + 1);
        }
        return response;
    }
}