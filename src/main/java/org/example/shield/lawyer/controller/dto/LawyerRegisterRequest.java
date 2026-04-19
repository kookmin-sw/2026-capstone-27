package org.example.shield.lawyer.controller.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LawyerRegisterRequest(
        @NotBlank(message = "변협 등록번호는 필수입니다")
        String barAssociationNumber,

        List<String> domains,
        List<String> subDomains,
        List<String> tags,
        Integer experienceYears,
        String bio,
        String region
) {}
