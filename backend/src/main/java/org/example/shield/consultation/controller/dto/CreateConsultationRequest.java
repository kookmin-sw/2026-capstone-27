package org.example.shield.consultation.controller.dto;

import java.util.List;

public record CreateConsultationRequest(
        List<String> domains,
        List<String> subDomains,
        List<String> tags
) {}
