package org.example.shield.brief.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.controller.dto.BriefResponse;
import org.example.shield.brief.controller.dto.BriefSummaryResponse;
import org.example.shield.brief.controller.dto.BriefUpdateRequest;
import org.example.shield.brief.controller.dto.BriefUpdateResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.domain.BriefWriter;
import org.example.shield.brief.exception.BriefAlreadyConfirmedException;
import org.example.shield.common.enums.PrivacySetting;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefService {

    private final BriefReader briefReader;
    private final BriefWriter briefWriter;

    public PageResponse<BriefSummaryResponse> getMyBriefs(UUID userId, Pageable pageable) {
        Page<Brief> briefs = briefReader.findAllByUserId(userId, pageable);
        Page<BriefSummaryResponse> responsePage = briefs.map(BriefSummaryResponse::from);
        return PageResponse.from(responsePage);
    }

    public BriefResponse getBrief(UUID briefId, UUID userId) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);
        return BriefResponse.from(brief);
    }

    @Transactional
    public BriefUpdateResponse updateBrief(UUID briefId, UUID userId, BriefUpdateRequest request) {
        Brief brief = briefReader.findById(briefId);
        validateOwner(brief, userId);

        if (!brief.isEditable()) {
            throw new BriefAlreadyConfirmedException();
        }

        // 내용 수정
        brief.updateContent(
                request.title() != null ? request.title() : brief.getTitle(),
                request.content() != null ? request.content() : brief.getContent(),
                request.keywords() != null ? request.keywords() : brief.getKeywords(),
                request.keyIssues() != null ? request.keyIssues() : brief.getKeyIssues(),
                brief.getStrategy()
        );

        // 개인정보 설정 변경
        if (request.privacySetting() != null) {
            try {
                brief.updatePrivacySetting(PrivacySetting.valueOf(request.privacySetting()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
            }
        }

        // 상태 변경
        if (request.status() != null) {
            switch (request.status()) {
                case "CONFIRMED" -> brief.confirm();
                case "DISCARDED" -> brief.discard();
                default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
            }
        }

        Brief saved = briefWriter.save(brief);

        return new BriefUpdateResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getUpdatedAt()
        );
    }

    private void validateOwner(Brief brief, UUID userId) {
        if (!brief.getUserId().equals(userId)) {
            throw new org.example.shield.brief.exception.BriefNotFoundException(brief.getId());
        }
    }
}
