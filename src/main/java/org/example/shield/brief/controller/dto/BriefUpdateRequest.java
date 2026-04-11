package org.example.shield.brief.controller.dto;

import java.util.List;

public record BriefUpdateRequest(
        String title,
        String content,
        List<String> keyIssues,
        List<String> keywords,
        String privacySetting,
        String status
) {}
