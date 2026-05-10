package org.example.shield.admin.controller.dto;

public record DashboardAlertsResponse(
        long overdueCount,
        long missingDocumentCount,
        long duplicateSuspectCount
) {}
