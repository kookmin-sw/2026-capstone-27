<div align="center">

# SHIELD

### AI 기반 법률 상담 자동 의뢰서 생성 플랫폼

변호사를 만나기 전, 사용자가 자신의 법적 상황을 정확히 정리하도록 돕는 RAG 기반 AI 법률 상담 서비스

[![Live](https://img.shields.io/badge/Live-shieldai.kr-2563EB?style=for-the-badge&logo=googlechrome&logoColor=white)](https://shieldai.kr)
[![Frontend](https://img.shields.io/badge/Frontend-SHIELD__FE-000000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/capstoneSHIELD/SHIELD_FE)
[![Backend](https://img.shields.io/badge/Backend-SHIELD__BE-000000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/capstoneSHIELD/SHIELD_BE)

**국민대학교 소프트웨어학부 캡스톤디자인 2026 — 27조**

</div>

---

## 1. 프로젝트 소개

법률 도움이 필요한 일반 시민이 변호사를 찾기 전에 겪는 두 가지 문제가 있습니다.

1. **무엇을 상담해야 할지 모른다** — 자기 상황이 어떤 분야(민사·이혼·임대차 등) 에 속하는지조차 불분명합니다.
2. **상담은 비싸고 시간은 짧다** — 정리되지 않은 채로 상담실에 들어가면 핵심을 다 말하지 못하고, 결국 재상담이 필요해집니다.

**SHIELD** 는 사용자가 채팅으로 자신의 상황을 설명하면, RAG (Retrieval-Augmented Generation) 기반 AI 가 관련 법령·판례를 검색해 단계적으로 질문을 던지며 **사건의 핵심을 정리한 의뢰서를 자동 생성** 합니다. 의뢰서는 분야별 전문 변호사에게 송부되고, 변호사가 수락하면 본격 상담으로 이어집니다.

기존 챗봇 서비스가 단답형 검색에 그치는 반면, SHIELD 는 **한국법제연구원(KLRI) LOD 온톨로지** 기반 3-level 법률 분류 체계 위에서 RAG 를 수행하여 사용자가 모르는 영역까지 후속 질문으로 끌어냅니다.

---

## 2. 주요 기능

### 의뢰인 (Client)
- **AI 법률 상담 챗봇** — 자연어 대화 → 법령·판례 RAG 검색 → 후속 질문 자동 생성
- **8개 분야 자동 분류** — 부동산 거래 · 이혼·위자료·재산분할 · 상속·유류분·유언 · 근로계약·해고·임금 · 손해배상·불법행위 · 채무·보증·개인파산·회생 · 임대차보호 · 기업·상사거래
- **의뢰서 자동 생성·송부** — 대화 내용을 구조화된 의뢰서(제목 · 내용 · 쟁점 · 키워드 · 분야) 로 변환하여 변호사에게 전달
- **진행률 시각화 / 분야 재선택** — 상담 진행도 표시, 도중에 분야 변경 가능

### 변호사 (Lawyer)
- **분야별 의뢰서 인박스** — 매칭된 의뢰서 수신, 24시간 응답 기한
- **수락·거절 워크플로** — 거절 사유 입력, 수락 시 진행 사건으로 자동 이동
- **사건 관리** — 진행 중 사건 목록·상세 조회

### 관리자 (Admin)
- **변호사 인증 심사** — 자격 서류 검토 → 승인 / 보완 요청 / 거절
- **운영 대시보드** — 24시간 미처리 의뢰 알림, 통계 확인

---

## 3. 시스템 구조

<div align="center">
  <img src="docs/architecture.png" alt="SHIELD 시스템 아키텍처" width="100%"/>
</div>

### RAG 파이프라인 — 3-way Hybrid Search

```
사용자 메시지
   │
   ▼
[Cohere embed-v4.0 임베딩 (1024차원)]
   │
   ▼
[PostgreSQL Hybrid Search]
 ├─ Vector cosine (pgvector <=>)  · weight 0.5
 ├─ BM25 (tsvector ts_rank)       · weight 0.3
 └─ pg_trgm similarity            · weight 0.2
   │
   ▼
[RagContextBuilder → 마크다운 컨텍스트]
   │
   ▼
[Cohere chat v2 + system prompt (분류 규약)]
   │
   ▼
구조화된 응답 (다음 질문 · 분야 분류 · 완료 여부)
```

민법 1,193개 조문 + 대법원 판례 데이터를 HNSW 인덱스 (m=16, ef_construction=64) 로 저장. 임베딩 호출 실패 시 자동으로 2-way (BM25 + trigram) 로 degrade 됩니다.

---

## 4. 기술 스택

### Frontend
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Tailwind](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![TanStack](https://img.shields.io/badge/TanStack_Query-FF4154?style=for-the-badge&logo=reactquery&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-000000?style=for-the-badge&logo=react&logoColor=white)

### Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### Database / AI
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector-336791?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Cohere](https://img.shields.io/badge/Cohere_API-39594D?style=for-the-badge&logo=cohere&logoColor=white)

### Auth
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Google](https://img.shields.io/badge/Google_OAuth-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Naver](https://img.shields.io/badge/Naver_Login-03C75A?style=for-the-badge&logo=naver&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao_Login-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)

### Infra / DevOps
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

### Collaboration
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)

---

## 5. 팀원 소개

<table>
  <tr>
    <td align="center" width="33%">
      <img src="docs/team/lee_cm.png" width="200" alt="이총명"/><br/>
      <b>이총명</b><br/>
      <sub>TODO</sub>
    </td>
    <td align="center" width="33%">
      <img src="docs/team/lee_sj.jpg" width="200" alt="이승진"/><br/>
      <b>이승진</b><br/>
      <sub>Backend</sub>
    </td>
    <td align="center" width="33%">
      <img src="docs/team/kang.jpg" width="200" alt="강문경"/><br/>
      <b>강문경</b><br/>
      <sub>Full-stack · PM</sub>
    </td>
  </tr>
</table>

---

## 6. 시작하기

### 사전 요구사항
- Java 21+
- Node.js 20+
- PostgreSQL 16 (with `pgvector` extension)
- Cohere API 키

### Backend
```bash
cd backend
cp deploy/shield.env.example .env
# .env 에 DB URL · Cohere API 키 · OAuth 시크릿 입력
./gradlew bootRun
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# https://localhost:5174 (basic SSL)
```

---

## 7. 링크

- **Live 서비스**: https://shieldai.kr
- **Backend 레포**: https://github.com/capstoneSHIELD/SHIELD_BE
- **Frontend 레포**: https://github.com/capstoneSHIELD/SHIELD_FE
