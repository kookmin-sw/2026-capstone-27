package org.example.shield.brief.controller.dto;

public record InboxStatsResponse(
        long total,
        long pending,
        long confirmed,
        long rejected
) {}
