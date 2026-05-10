// ── Types ──────────────────────────────────────────────────────────────────

export interface SpecLeaf {
  name: string;
  aliases: string[];
}

export interface SpecLevel2 {
  name: string;
  children: SpecLeaf[];
}

export interface SpecLevel1 {
  name: string;
  children: SpecLevel2[];
}

export interface SearchResult {
  leaf: SpecLeaf;
  path: [string, string, string]; // [level1, level2, level3]
}

// ── Data ───────────────────────────────────────────────────────────────────

export const SPECIALIZATION_TREE: SpecLevel1[] = [
  {
    name: '부동산 거래',
    children: [
      {
        name: '부동산 매매',
        children: [
          { name: '계약 체결 및 효력', aliases: ['부동산 매매계약', '매매계약 체결', '매매계약 효력', '부동산 계약서'] },
          { name: '대금 지급 및 정산', aliases: ['매매대금 지급', '잔금 지급', '대금 정산', '계약금 중도금 잔금'] },
          { name: '계약 해제·해지', aliases: ['매매계약 해제', '매매계약 해지', '계약 파기', '계약취소'] },
          { name: '하자담보책임', aliases: ['부동산 하자', '하자 보수', '하자담보 청구', '누수 하자', '곰팡이 하자'] },
          { name: '소유권 이전 및 등기', aliases: ['소유권이전등기', '소유권 이전 청구', '소유권 이전 등기 지연', '이전등기 소송'] },
          { name: '명의신탁 및 이중매매', aliases: ['부동산 명의신탁', '이중매매 분쟁', '명의신탁 해지', '명의신탁 소송'] },
        ],
      },
      {
        name: '부동산 임대차',
        children: [
          { name: '계약 체결 및 조건', aliases: ['임대차계약 체결', '임대차 계약서', '임대 조건', '전세계약 체결'] },
          { name: '보증금 및 차임', aliases: ['보증금 반환', '월세 체납', '전세 보증금', '차임 연체'] },
          { name: '계약 갱신 및 종료', aliases: ['계약 갱신 거절', '묵시적 갱신', '임대차 종료', '재계약'] },
          { name: '명도 및 인도', aliases: ['명도소송', '건물 인도 청구', '퇴거 요구', '퇴실 소송'] },
          { name: '수선·원상회복 및 비용', aliases: ['원상복구', '수리 비용 부담', '수선 의무', '인테리어 원상복구'] },
        ],
      },
      {
        name: '부동산 담보',
        children: [
          { name: '저당권 및 근저당권', aliases: ['근저당 설정', '근저당 말소', '저당권 설정', '저당권 실행'] },
          { name: '용익물권 및 전세권', aliases: ['전세권 설정', '전세권 말소', '법정지상권', '지역권 지상권'] },
          { name: '담보권 실행 및 경매', aliases: ['부동산 경매', '임의경매', '강제경매', '담보권 실행'] },
          { name: '배당 및 우선순위', aliases: ['배당요구', '배당이의', '우선순위 다툼', '저당 순위'] },
          { name: '담보권 말소 및 변경', aliases: ['근저당 말소 청구', '담보 말소', '담보 변경', '담보 해지'] },
        ],
      },
      {
        name: '부동산 권리관계',
        children: [
          { name: '공유 및 지분관계', aliases: ['공유지분', '공유물분할', '지분권 분쟁'] },
          { name: '경계 및 인접관계', aliases: ['토지 경계', '경계침범', '담장 경계 분쟁'] },
          { name: '점유 및 인도청구', aliases: ['점유 회복', '점유 방해', '인도 청구 소송'] },
          { name: '권리귀속 및 확인', aliases: ['소유권 확인', '권리귀속 확인', '소유자 다툼'] },
        ],
      },
    ],
  },
  {
    name: '이혼·위자료·재산분할',
    children: [
      {
        name: '이혼 절차',
        children: [
          { name: '협의이혼', aliases: ['협의 이혼 절차', '협의이혼 신청', '협의이혼 준비'] },
          { name: '재판상 이혼', aliases: ['소송 이혼', '재판 이혼', '이혼소송 제기'] },
          { name: '이혼 사유 및 책임', aliases: ['유책배우자', '이혼 사유', '누가 잘못인지'] },
          { name: '혼인무효 및 취소', aliases: ['혼인 무효소송', '혼인 취소소송', '위장 결혼'] },
        ],
      },
      {
        name: '위자료',
        children: [
          { name: '청구 요건 및 책임주체', aliases: ['위자료 청구 요건', '누구에게 위자료 청구', '위자료 책임자'] },
          { name: '산정 기준', aliases: ['위자료 액수', '위자료 얼마나', '위자료 기준', '정신적 손해배상 기준'] },
          { name: '제3자 책임', aliases: ['상간자 위자료', '상간남 위자료', '상간녀 소송'] },
        ],
      },
      {
        name: '재산분할',
        children: [
          { name: '분할 대상 재산 특정', aliases: ['재산분할 대상', '어떤 재산 나누는지', '부부 재산 목록'] },
          { name: '기여도 산정', aliases: ['기여도 평가', '가사노동 기여', '재산형성 기여도'] },
          { name: '채무 분담', aliases: ['부부 빚 분담', '채무 재산분할', '대출 분담'] },
          { name: '분할 방법 및 집행', aliases: ['현물분할', '금전분할', '재산분할 소송 집행'] },
        ],
      },
      {
        name: '자녀 및 양육',
        children: [
          { name: '친권 및 양육권', aliases: ['양육권 분쟁', '친권자 지정', '아이 누가 키우는지'] },
          { name: '양육비 산정 및 청구', aliases: ['양육비 청구', '양육비 얼마나', '양육비 안 줄 때'] },
          { name: '면접교섭권', aliases: ['면접교섭 제한', '아이 면담', '아이 만나는 권리'] },
        ],
      },
    ],
  },
  {
    name: '상속·유류분·유언',
    children: [
      {
        name: '상속 일반',
        children: [
          { name: '상속순위 및 상속분', aliases: ['누가 얼마 상속', '상속순위', '상속지분 비율'] },
          { name: '대습상속 및 결격', aliases: ['대습상속', '상속결격', '상속자 결격 사유'] },
          { name: '상속재산의 범위', aliases: ['상속재산 목록', '어떤 재산 상속', '숨은 재산'] },
          { name: '특별수익 및 기여분', aliases: ['생전증여 반영', '특별수익 재산', '기여분 인정'] },
        ],
      },
      {
        name: '상속재산 처리',
        children: [
          { name: '상속재산 분할', aliases: ['상속재산분할', '상속 재산 나누기', '상속분할 소송'] },
          { name: '상속채무 및 청산', aliases: ['상속빚', '상속채무 상속', '빚 정리'] },
          { name: '상속포기', aliases: ['상속 포기', '상속포기 신청', '빚 상속 안 받기'] },
          { name: '한정승인', aliases: ['한정승인 신청', '빚 한정승인', '상속한정승인'] },
        ],
      },
      {
        name: '유언',
        children: [
          { name: '유언 방식 및 요건', aliases: ['유언장 작성', '자필증서유언', '공정증서유언'] },
          { name: '유언 효력 및 무효', aliases: ['유언장 효력', '유언 무효', '유언장 취소'] },
          { name: '유언 집행', aliases: ['유언 집행자', '유언대로 재산 분배', '유언 이행'] },
        ],
      },
      {
        name: '유류분',
        children: [
          { name: '유류분 권리자 및 비율', aliases: ['유류분 권리자', '유류분 비율', '최소 상속분'] },
          { name: '유류분 산정', aliases: ['유류분 계산', '유류분액 산정', '유류분 얼마'] },
          { name: '증여·유증과 유류분', aliases: ['생전증여 유류분', '유증과 유류분', '편중 상속'] },
          { name: '유류분 반환청구', aliases: ['유류분 반환 소송', '유류분 청구', '유류분 소송'] },
        ],
      },
    ],
  },
  {
    name: '근로계약·해고·임금',
    children: [
      {
        name: '근로계약',
        children: [
          { name: '계약 성립 및 근로조건', aliases: ['근로계약서', '근로조건 명시', '연봉계약', '근로계약 체결'] },
          { name: '고용형태 및 비정규직', aliases: ['기간제 근로자', '파견 근로자', '비정규직 처우'] },
          { name: '근로자성 판단', aliases: ['프리랜서 근로자성', '특수고용 노동자', '실질 근로자 여부'] },
        ],
      },
      {
        name: '임금 및 수당',
        children: [
          { name: '임금체불 및 지급청구', aliases: ['월급 밀림', '임금 체불 신고', '체불임금 소송'] },
          { name: '통상임금 및 평균임금', aliases: ['통상임금 포함 항목', '평균임금 계산', '임금 산정 기준'] },
          { name: '법정수당 산정', aliases: ['연장근로수당', '야근수당', '휴일근로수당'] },
          { name: '퇴직금 및 퇴직급여', aliases: ['퇴직금 계산', '퇴직금 미지급', '퇴직금 청구'] },
        ],
      },
      {
        name: '근로시간 및 휴가',
        children: [
          { name: '근로시간 및 연장근로', aliases: ['주52시간제', '초과근로', '야근 시간 제한'] },
          { name: '휴게·휴일 및 연차휴가', aliases: ['연차 사용', '연차수당', '주휴수당'] },
          { name: '휴직 및 복직', aliases: ['육아휴직', '병가', '복직 거부'] },
        ],
      },
      {
        name: '해고 및 징계',
        children: [
          { name: '징계 사유 및 절차', aliases: ['징계위원회', '감봉 정직 해고', '징계 적법성'] },
          { name: '해고의 정당성', aliases: ['정당한 해고', '해고 사유', '해고 정당한지'] },
          { name: '경영상 해고', aliases: ['정리해고', '구조조정 해고', '경영상 이유 해고'] },
          { name: '부당해고 구제', aliases: ['부당해고 판정', '부당해고 구제신청', '복직 명령'] },
        ],
      },
      {
        name: '직장 내 권리보호',
        children: [
          { name: '직장 내 괴롭힘', aliases: ['업무상 괴롭힘', '왕따', '직장 갑질'] },
          { name: '성희롱 및 차별', aliases: ['직장 내 성희롱', '성차별', '채용 차별'] },
          { name: '모성보호 및 일·가정 양립', aliases: ['임신 해고', '출산휴가', '육아휴직 차별'] },
          { name: '산업재해 및 보상', aliases: ['산재 인정', '산재 보험', '업무상 재해'] },
        ],
      },
    ],
  },
  {
    name: '손해배상·불법행위',
    children: [
      {
        name: '손해배상 일반',
        children: [
          { name: '불법행위 성립요건', aliases: ['불법행위 요건', '위법성', '인과관계', '과실'] },
          { name: '재산적 손해 산정', aliases: ['치료비 손해', '수리비 손해', '일실수익 계산'] },
          { name: '정신적 손해 및 위자료', aliases: ['위자료', '정신적 손해배상', '위자료 산정'] },
          { name: '과실상계 및 책임제한', aliases: ['과실상계', '책임제한', '공동과실'] },
        ],
      },
      {
        name: '교통사고',
        children: [
          { name: '과실비율 산정', aliases: ['과실비율', '과실비', '몇 대 몇 과실'] },
          { name: '보험 합의 및 소송', aliases: ['보험사 합의', '합의금 분쟁', '교통사고 소송'] },
          { name: '후유장해 및 일실수익', aliases: ['후유장해 평가', '장해등급', '일실수입 산정'] },
        ],
      },
      {
        name: '의료사고',
        children: [
          { name: '진료 과실 및 설명의무', aliases: ['의료과실', '설명의무 위반', '수술 설명 부족'] },
          { name: '인과관계 및 입증책임', aliases: ['의료사고 입증', '인과관계 증명', '환자 입증책임'] },
          { name: '의료분쟁 조정 및 소송', aliases: ['의료분쟁조정', '의료중재', '의료소송'] },
        ],
      },
      {
        name: '인격권 침해',
        children: [
          { name: '명예훼손 및 모욕', aliases: ['명예훼손 소송', '모욕죄', '비방글 손해배상'] },
          { name: '초상권 및 사생활 침해', aliases: ['초상권 침해', '사생활 침해', '불법 촬영 공개'] },
          { name: '개인정보 침해', aliases: ['개인정보 유출', '개인정보 손해배상', '개인정보보호법 위반'] },
        ],
      },
      {
        name: '특수 불법행위 책임',
        children: [
          { name: '제조물 책임', aliases: ['제품결함 책임', 'PL법', '결함 제품 손해배상'] },
          { name: '시설물 관리 책임', aliases: ['시설물 사고', '건물 관리 책임', '공사장 사고'] },
          { name: '사용자 책임', aliases: ['사용자 책임', '직원 사고 책임', '업무 중 사고 책임'] },
        ],
      },
    ],
  },
  {
    name: '채무·보증·개인파산·회생',
    children: [
      {
        name: '금전채권 및 채무',
        children: [
          { name: '대여금 및 이자', aliases: ['빌려준 돈', '대여금 소송', '사채 이자'] },
          { name: '지연손해금 청구', aliases: ['연체이자', '지연이자', '지연손해금 약정'] },
          { name: '변제·상계·공탁', aliases: ['변제 공탁', '상계 주장', '돈 갚는 방법'] },
          { name: '채권양도 및 채무인수', aliases: ['채권양도 통지', '채무자 변경', '채무인수 계약'] },
          { name: '소멸시효', aliases: ['채권 소멸시효', '시효 완성', '시효 중단'] },
        ],
      },
      {
        name: '보증',
        children: [
          { name: '보증계약 성립 및 유형', aliases: ['연대보증', '근보증', '보증 계약'] },
          { name: '보증책임 범위 및 한도', aliases: ['보증 한도', '보증 기간', '보증 책임 범위'] },
          { name: '보증인 구상권', aliases: ['구상금 청구', '보증인 구상', '보증인 되갚기 청구'] },
        ],
      },
      {
        name: '민사집행 및 보전처분',
        children: [
          { name: '지급명령 및 소액사건', aliases: ['지급명령 신청', '소액사건 소송', '간이 소송'] },
          { name: '가압류 및 가처분', aliases: ['가압류 신청', '가처분 신청', '보전처분'] },
          { name: '강제집행 절차', aliases: ['급여 압류', '계좌 압류', '강제집행 신청'] },
          { name: '불법 채권추심 대응', aliases: ['불법추심', '채권추심 협박', '추심 대응'] },
        ],
      },
      {
        name: '개인파산',
        children: [
          { name: '파산 신청 요건', aliases: ['개인파산 자격', '파산 신청 조건', '빚 탕감 가능 여부'] },
          { name: '파산 절차', aliases: ['파산 진행 절차', '파산신청 방법', '파산 재판'] },
          { name: '면책 및 불허가 사유', aliases: ['면책결정', '면책 불허가', '면책 취소 사유'] },
        ],
      },
      {
        name: '개인회생',
        children: [
          { name: '회생 신청 요건', aliases: ['개인회생 자격', '회생 신청 조건', '최대 채무 한도'] },
          { name: '변제계획안 및 인가', aliases: ['변제계획안 작성', '변제계획 인가', '회생 변제율'] },
          { name: '회생 면책', aliases: ['회생 후 면책', '회생 종료', '회생 실패'] },
        ],
      },
    ],
  },
  {
    name: '임대차보호',
    children: [
      {
        name: '주택임대차보호',
        children: [
          { name: '대항력 및 우선변제권', aliases: ['대항력 요건', '우선변제권 확보', '확정일자 전입신고'] },
          { name: '소액임차인 최우선변제', aliases: ['소액보증금', '최우선변제권', '소액임차인 보호'] },
          { name: '계약갱신요구권', aliases: ['계약 갱신 요구', '갱신 거절 사유', '계약갱신청구권'] },
          { name: '차임 증감 제한', aliases: ['전세금 인상', '월세 인상 제한', '임대료 상한'] },
          { name: '보증금 반환 및 회수', aliases: ['보증금 못 받는 경우', '보증금 반환 소송', '보증금 회수 방법'] },
        ],
      },
      {
        name: '상가건물임대차보호',
        children: [
          { name: '적용범위 및 요건', aliases: ['환산보증금 기준', '상가임대차 적용범위', '보호대상 상가'] },
          { name: '상가 계약갱신요구권', aliases: ['상가 계약갱신', '10년 갱신요구', '갱신 거절'] },
          { name: '권리금 보호', aliases: ['권리금 회수', '권리금 방해', '권리금 계약'] },
          { name: '상가 차임 증감 제한', aliases: ['상가 임대료 인상', '상가 차임 증액 제한', '보증금 증액'] },
        ],
      },
      {
        name: '임차인 보호 절차',
        children: [
          { name: '임차권등기명령', aliases: ['임차권 등기', '임차권등기명령 신청', '이사 전 등기'] },
          { name: '임대차 분쟁조정', aliases: ['임대차분쟁조정위원회', '분쟁조정 신청', '집주인 분쟁 조정'] },
          { name: '보증금 반환 소송', aliases: ['보증금 반환 청구', '전세금 반환 소송', '보증금 소송'] },
          { name: '명도소송 대응', aliases: ['명도 요구 대응', '내쫓기기 방어', '점유이전금지가처분'] },
        ],
      },
    ],
  },
  {
    name: '기업·상사거래',
    children: [
      {
        name: '상사계약 일반',
        children: [
          { name: '계약 체결 및 해석', aliases: ['상사계약 체결', '계약 해석 분쟁', '계약 조항 해석'] },
          { name: '계약 이행 및 위반', aliases: ['계약 불이행', '계약위반 손해배상', '채무불이행'] },
          { name: '상사 대금 지급 및 정산', aliases: ['대금 미지급', '어음결제', '상사대금 청구'] },
          { name: '해제·해지 및 손해배상', aliases: ['계약 해제', '계약 해지', '손해배상 청구'] },
        ],
      },
      {
        name: '납품·공급·도급',
        children: [
          { name: '물품공급 및 납품계약', aliases: ['납품계약', '공급계약', '납기 지연'] },
          { name: '용역 및 위탁계약', aliases: ['용역계약', '위탁계약', '용역 미이행'] },
          { name: '도급 및 제작계약', aliases: ['도급계약', '공사도급', '제작 지연'] },
          { name: '검수·인수 및 지체', aliases: ['검수 불합격', '인수 거부', '지체상금'] },
          { name: '하자담보 및 클레임', aliases: ['하자 클레임', '품질 하자', 'A/S 책임'] },
        ],
      },
      {
        name: '유통·가맹·대리점',
        children: [
          { name: '가맹계약 및 정보공개', aliases: ['프랜차이즈 계약', '정보공개서', '가맹사업법'] },
          { name: '대리점 및 총판계약', aliases: ['대리점 계약', '총판 계약', '독점 대리점'] },
          { name: '거래중단 및 계약종료', aliases: ['거래 중단 통보', '계약 해지 통보', '물량 끊김'] },
          { name: '불공정거래행위', aliases: ['갑질 거래', '불공정거래', '우월적 지위 남용'] },
        ],
      },
      {
        name: '지분 및 주주 분쟁',
        children: [
          { name: '주식 양도 및 발행', aliases: ['주식양도', '신주발행', '주식 명의개서'] },
          { name: '주주총회 결의 하자', aliases: ['주주총회 무효', '결의 취소', '총회 절차 하자'] },
          { name: '주주간 계약 분쟁', aliases: ['주주간계약', '동반매도청구', '주식매수청구'] },
          { name: '소수주주권 행사', aliases: ['소수주주권', '회계장부 열람', '주주제안권'] },
          { name: '경영권 분쟁', aliases: ['경영권 다툼', '대표이사 해임', '직무집행정지 가처분'] },
        ],
      },
      {
        name: '임원 책임 및 영업보호',
        children: [
          { name: '이사·임원 책임', aliases: ['임원 책임', '선관주의의무', '충실의무 위반'] },
          { name: '주주대표소송', aliases: ['주주대표소송 제기', '회사에 대한 손해배상청구', '이사 책임 추궁'] },
          { name: '영업비밀 보호', aliases: ['기밀 유지', '영업비밀 유출', '부정경쟁방지법'] },
          { name: '경업금지 및 전직금지', aliases: ['경업금지약정', '전직금지약정', '퇴사 후 경쟁사 이직'] },
        ],
      },
    ],
  },
];

// ── Search utility ─────────────────────────────────────────────────────────

/**
 * 리프(L3) 태그 이름 배열을 받아 조상 추적을 통해
 * { domains(L1), subDomains(L2), tags(L3) } 을 리턴한다.
 *
 * 중복 제거 + 정규화된 순서 유지. 매칭 실패 리프는 tags 에만 남긴다
 * (상위 없이도 서버가 별도 저장할 수 있도록 허용).
 */
export function resolveSpecHierarchy(leafTags: string[]): {
  domains: string[];
  subDomains: string[];
  tags: string[];
} {
  const domainSet = new Set<string>();
  const subDomainSet = new Set<string>();
  const tagSet = new Set<string>();

  for (const tag of leafTags) {
    if (!tag) continue;
    tagSet.add(tag);

    outer: for (const l1 of SPECIALIZATION_TREE) {
      for (const l2 of l1.children) {
        if (l2.children.some((leaf) => leaf.name === tag)) {
          domainSet.add(l1.name);
          subDomainSet.add(l2.name);
          break outer;
        }
      }
    }
  }

  return {
    domains: Array.from(domainSet),
    subDomains: Array.from(subDomainSet),
    tags: Array.from(tagSet),
  };
}

export function searchSpecializations(query: string): SearchResult[] {
  if (!query.trim()) return [];

  const q = query.trim().toLowerCase();
  const results: SearchResult[] = [];

  for (const l1 of SPECIALIZATION_TREE) {
    for (const l2 of l1.children) {
      for (const leaf of l2.children) {
        const nameMatch = leaf.name.toLowerCase().includes(q);
        const aliasMatch = leaf.aliases.some((a) => a.toLowerCase().includes(q));
        if (nameMatch || aliasMatch) {
          results.push({ leaf, path: [l1.name, l2.name, leaf.name] });
        }
      }
    }
  }

  return results;
}
