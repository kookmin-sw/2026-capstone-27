package org.example.shield.admin.controller.dto;

public record DashboardStatsResponse(
        long pendingCount,
        long reviewingCount,
        long supplementRequestedCount,
        long todayProcessedCount
) {}
