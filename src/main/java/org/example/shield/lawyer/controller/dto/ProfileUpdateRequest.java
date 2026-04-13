package org.example.shield.lawyer.controller.dto;

import java.util.List;

public record ProfileUpdateRequest(
        String specializations,
        Integer experienceYears,
        List<String> certifications,
        List<String> tags,
        String bio,
        String region
) {}
