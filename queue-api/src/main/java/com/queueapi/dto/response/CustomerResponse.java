package com.queueapi.dto.response;

import com.queueapi.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String name;
    private String phone;
    private QueueStatus status;
    private Instant createdAt;
    private Instant calledAt;
    private Instant completedAt;
    private Long position;
}