package com.queueapi.service;


import com.queueapi.dto.request.CustomerCreateRequest;
import com.queueapi.dto.response.CustomerResponse;
import com.queueapi.dto.response.QueueStatsResponse;
import com.queueapi.enums.QueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QueueService {
    CustomerResponse addCustomer(CustomerCreateRequest request);
    List<CustomerResponse> getWaitingCustomers();
    CustomerResponse getCustomer(UUID id);
    CustomerResponse callNext();
    void removeCustomer(UUID id);
    CustomerResponse completeCurrent(UUID id);
    CustomerResponse cancel(UUID id);
    QueueStatsResponse getStats();
    Page<CustomerResponse> getByStatus(QueueStatus status, Pageable pageable);
}