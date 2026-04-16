package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.KeyIssue;

import java.util.List;

public record BriefUpdateRequest(
        String title,
        String content,
        List<KeyIssue> keyIssues,
        List<String> keywords,
        String privacySetting,
        String status
) {}
