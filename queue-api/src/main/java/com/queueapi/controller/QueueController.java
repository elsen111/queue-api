package com.queueapi.controller;

import com.queueapi.common.response.ApiResponse;
import com.queueapi.dto.request.CustomerCreateRequest;
import com.queueapi.dto.response.CustomerResponse;
import com.queueapi.dto.response.QueueStatsResponse;
import com.queueapi.enums.QueueStatus;
import com.queueapi.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "Customer queue management")
public class QueueController {

    private final QueueService queueService;

    @PostMapping
    @Operation(summary = "Add a new customer to the queue")
    public ApiResponse<CustomerResponse> addCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return ApiResponse.success("Customer added to queue", queueService.addCustomer(request));
    }

    @GetMapping
    @Operation(summary = "List waiting customers, paginated and ordered by position")
    public ApiResponse<Page<CustomerResponse>> getWaiting(Pageable pageable) {
        return ApiResponse.success(queueService.getWaitingCustomers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer by id, including their queue position")
    public ApiResponse<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ApiResponse.success(queueService.getCustomer(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/position")
    @Operation(summary = "Get only the queue position of a customer")
    public ApiResponse<Long> getPosition(@PathVariable UUID id) {
        return ApiResponse.success(queueService.getCustomer(id).getPosition());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    @Operation(summary = "Get queue statistics (admin only)")
    public ApiResponse<QueueStatsResponse> getStats() {
        return ApiResponse.success(queueService.getStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/history")
    @Operation(summary = "Paginated history by status, e.g. COMPLETED or CANCELLED (admin only)")
    public ApiResponse<Page<CustomerResponse>> getHistory(@RequestParam QueueStatus status, Pageable pageable) {
        return ApiResponse.success(queueService.getByStatus(status, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/next")
    @Operation(summary = "Call the next waiting customer (admin only)")
    public ApiResponse<CustomerResponse> callNext() {
        return ApiResponse.success("Next customer called", queueService.callNext());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark a currently-served customer as completed (admin only)")
    public ApiResponse<CustomerResponse> complete(@PathVariable UUID id) {
        return ApiResponse.success("Customer marked as completed", queueService.completeCurrent(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a customer's place in the queue (admin only)")
    public ApiResponse<CustomerResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Customer cancelled", queueService.cancel(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a customer from the queue entirely (admin only)")
    public void remove(@PathVariable UUID id) {
        queueService.removeCustomer(id);
    }
}