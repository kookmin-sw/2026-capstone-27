package org.example.shield.brief.application;

import java.util.UUID;

public record DeliveryStatusEvent(
        UUID clientUserId,
        String clientEmail,
        String clientName,
        String lawyerName,
        String briefTitle,
        String status,
        String rejectionReason
) {}
