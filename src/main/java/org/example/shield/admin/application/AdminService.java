package org.example.shield.admin.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.admin.controller.dto.PendingLawyerResponse;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.infrastructure.LawyerDocumentRepository;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final LawyerReader lawyerReader;
    private final UserReader userReader;
    private final LawyerDocumentRepository lawyerDocumentRepository;

    public PageResponse<PendingLawyerResponse> getPendingLawyers(String keyword, String status,
                                                                  Pageable pageable) {
        log.info("변호사 심사 목록 조회. keyword={}, status={}", keyword, status);

        Page<LawyerProfile> lawyerPage = lawyerReader.searchByStatusAndKeyword(
                status, keyword, pageable);

        List<LawyerProfile> lawyers = lawyerPage.getContent();
        if (lawyers.isEmpty()) {
            return PageResponse.from(lawyerPage.map(lp -> null));
        }

        // N+1 방지: batch fetch users
        List<UUID> userIds = lawyers.stream()
                .map(LawyerProfile::getUserId)
                .toList();
        Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // N+1 방지: batch fetch document counts
        List<UUID> lawyerIds = lawyers.stream()
                .map(LawyerProfile::getId)
                .toList();
        Map<UUID, Long> documentCountMap = lawyerDocumentRepository.countByLawyerIds(lawyerIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        Page<PendingLawyerResponse> responsePage = lawyerPage.map(lawyer -> {
            User user = userMap.get(lawyer.getUserId());
            long docCount = documentCountMap.getOrDefault(lawyer.getId(), 0L);
            return PendingLawyerResponse.from(lawyer, user, docCount);
        });

        return PageResponse.from(responsePage);
    }

}
