package org.example.shield.consultation.controller.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ClassifyRequest(
        @NotEmpty(message = "분류 값은 필수입니다")
        List<String> primaryField
) {}
