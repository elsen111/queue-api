package com.queueapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QueueStatsResponse {
    private long totalWaiting;
    private long totalServing;
    private long totalCompleted;
    private long totalCancelled;
    private CustomerResponse currentlyServing;
}