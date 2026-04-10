package org.example.shield.brief.controller.dto;

import java.util.List;
import java.util.UUID;

public record MatchingResponse(
        UUID lawyerId,
        String name,
        String profileImageUrl,
        String specializations,
        Integer experienceYears,
        List<String> tags,
        List<String> matchedKeywords
) {}
