package org.example.shield.consultation.controller.dto;

import org.example.shield.common.enums.DomainType;

public record CreateConsultationRequest(
        DomainType domain
) {}
