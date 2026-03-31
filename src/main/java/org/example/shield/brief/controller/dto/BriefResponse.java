package org.example.shield.brief.controller.dto;

/**
 * 의뢰서 조회 응답 DTO.
 *
 * TODO:
 * - briefId: String (UUID)
 * - title: String ("[노동] 부당해고 관련 의뢰서")
 * - legalField: String
 * - content: String (줄글 의뢰서 전체 내용)
 * - keywords: List<String> (매칭용 키워드)
 * - privacySetting: String (FULL / PARTIAL / PRIVATE)
 * - status: String (DRAFT / CONFIRMED / DELIVERED / DISCARDED)
 * - createdAt: LocalDateTime
 */
public class BriefResponse {
}
