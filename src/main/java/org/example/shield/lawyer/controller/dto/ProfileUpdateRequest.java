package org.example.shield.lawyer.controller.dto;

import java.util.List;

public record ProfileUpdateRequest(
        List<String> domains,
        List<String> subDomains,
        Integer experienceYears,
        List<String> certifications,
        List<String> tags,
        String bio,
        String region
) {}
