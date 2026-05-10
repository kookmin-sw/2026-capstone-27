# Figma Design Reference

이 폴더에는 Figma MCP `get_design_context` API로 추출한 디자인 정답지 파일이 포함되어 있습니다.

## 용도

- 현재 구현 코드와 Figma 원본 디자인 간의 시각적 차이를 비교하는 참조 자료
- UI realign 작업 시 ground truth로 사용
- 코드에서 직접 import하지 않음 (런타임에 사용되지 않음)

## 파일 형식

- `.html` — 초기에 가져온 파일 (Tailwind CDN 포함, 브라우저에서 직접 열기 가능)
- `.jsx` — 이후에 가져온 파일 (React JSX + Tailwind 클래스, 참조용)

## 원본 Figma 파일

- File Key: `6eFZg6uOGTZiiNZTA8YyGF`
- 이름: 캡스톤 쉴드 와이어프레임

## 화면 매핑

| 파일 | Figma 화면 | 대응 구현 파일 |
| ---- | ---- | ---- |
| splash_page.html | 스플래시 | src/routes/auth/SplashPage.tsx |
| login_page.html | 로그인 | src/routes/auth/LoginPage.tsx |
| role_selection.html | 역할 선택 | src/routes/auth/RoleSelectPage.tsx |
| client_signup.html | 의뢰인 가입 | src/routes/auth/ClientRegisterPage.tsx |
| lawyer_signup.html | 변호사 가입 | src/routes/auth/LawyerRegisterPage.tsx |
| manual_field_selection.html | 분야 선택 | src/routes/client/NewConsultationPage.tsx |
| chat_consultation.html | 채팅 상담 | src/routes/client/ChatPage.tsx |
| classification_results.html | 분류 결과 | src/routes/client/AnalyzingPage.tsx |
| processing_case.html | 분석 중 | src/routes/client/AnalyzingPage.tsx |
| case_analysis.jsx | 분석 리포트 | src/routes/client/BriefDetailPage.tsx |
| privacy_settings.jsx | 개인정보 설정 | src/routes/client/PrivacySettingsPage.tsx |
| final_review.jsx | 최종 확인 | src/routes/client/FinalReviewPage.tsx |
| lawyer_search_results.jsx | 변호사 찾기 | src/routes/client/LawyerListPage.tsx |
| lawyer_profile.jsx | 변호사 프로필 | src/routes/client/LawyerProfilePage.tsx |
| request_confirmation.jsx | 전달 확인 | src/routes/client/RequestConfirmPage.tsx |
| request_tracking.jsx | 의뢰 현황 | src/routes/client/RequestTrackingPage.tsx |
| admin_dashboard.jsx | 관리자 콘솔 | src/routes/admin/AdminDashboardPage.tsx |
| admin_application_list.jsx | 가입 심사 | src/routes/admin/LawyerPendingPage.tsx |
| admin_application_detail.jsx | 신청 상세 | src/routes/admin/LawyerReviewPage.tsx |
| admin_history.jsx | 처리 이력 | src/routes/admin/LogsPage.tsx |
| lawyer_dashboard.jsx | 변호사 대시보드 | src/routes/lawyer/DashboardPage.tsx |
| lawyer_inbox.jsx | 의뢰함 | src/routes/lawyer/InboxPage.tsx |
| lawyer_inbox_detail.jsx | 의뢰 상세 | src/routes/lawyer/InboxDetailPage.tsx |
| lawyer_my_profile.jsx | 내 프로필 | src/routes/lawyer/ProfileEditPage.tsx |
