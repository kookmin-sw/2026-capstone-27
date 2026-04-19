// Figma Node: 1:1671 - Lawyer Profile (390x1150)
// Source: get_design_context from fileKey 6eFZg6uOGTZiiNZTA8YyGF
// Full raw JSX preserved as-is

const imgAvatar = "https://www.figma.com/api/mcp/asset/a88cffdb-8f24-44b1-8800-cfa7d7f4e57f";
const imgGroup = "https://www.figma.com/api/mcp/asset/b704ea2e-1536-40a0-9dae-e77f7cbbffdd";
const imgGroup1 = "https://www.figma.com/api/mcp/asset/28e07925-176d-4637-a989-ef717577e906";
const imgGroup2 = "https://www.figma.com/api/mcp/asset/16960bd4-88aa-4916-9c0a-89dc30a7c5ac";
const imgGroup3 = "https://www.figma.com/api/mcp/asset/f8e0f6c2-53bf-4138-9323-db0f43ae5a5f";
const imgGroup4 = "https://www.figma.com/api/mcp/asset/b89296f1-ee37-4ad8-b02a-3d303701f8f4";
const imgGroup5 = "https://www.figma.com/api/mcp/asset/eacc9a91-ea63-488d-b44d-b30a22000f1c";
const imgGroup6 = "https://www.figma.com/api/mcp/asset/5b499d62-5bcf-401e-84e1-75039acab3f5";
const imgGroup7 = "https://www.figma.com/api/mcp/asset/d69e00f3-17a1-4406-b225-32aa487366f9";
const imgGroup8 = "https://www.figma.com/api/mcp/asset/fdf1d161-0072-4d9b-8c50-c07b3f7211c9";
const imgGroup9 = "https://www.figma.com/api/mcp/asset/b3241cc8-b039-48f3-bef0-700ba9a36f3a";
const imgGroup10 = "https://www.figma.com/api/mcp/asset/d72d41e2-b2e8-45b8-8b31-49e86bcf6846";
const imgLine = "https://www.figma.com/api/mcp/asset/4b51c4fc-0783-41ea-aec8-5afebb0990d4";
const imgGroup14 = "https://www.figma.com/api/mcp/asset/8d675846-4342-4df8-8978-409b3d5ccc74";
const imgLine1 = "https://www.figma.com/api/mcp/asset/2a58a428-4194-4927-be76-67f9bf49a145";

export default function LawyerProfile() {
  return (
    <div className="bg-white relative shadow-[0px_3px_6px_0px_rgba(18,15,40,0.12)] size-full" data-node-id="1:1671" data-name="Lawyer Profile">
      <div className="absolute h-256.5 left-[0.5px] overflow-clip top-[103.5px] w-97.5" data-node-id="1:1672">
        {/* 프로필 헤더 */}
        <div className="absolute bg-[#f0e4fc] left-6 overflow-clip rounded-[40px] size-20 top-8">
          <img alt="변호사 프로필" className="absolute inset-0 w-full h-full object-cover" src={imgAvatar} />
        </div>
        <p className="absolute font-bold left-30 text-[#16181d] text-[20px] top-8">김성민 변호사</p>
        <div className="absolute bg-[#f0f7ff] h-5 left-60 overflow-clip rounded-[10px] top-8 w-14.25">
          <p className="absolute font-medium left-1.25 text-[#0680f9] text-[12px] top-0">경력 15년</p>
        </div>
        <div className="absolute flex gap-1 left-30 top-17.5">
          <span className="bg-[#f3f4f6] rounded-lg px-2 text-[#575e6b] text-[12px]">부동산</span>
          <span className="bg-[#f3f4f6] rounded-lg px-2 text-[#575e6b] text-[12px]">임대차</span>
          <span className="bg-[#f3f4f6] rounded-lg px-2 text-[#575e6b] text-[12px]">민사집행</span>
        </div>
        {/* 매칭 키워드 */}
        <div className="absolute bg-[#f0f7ff] h-44.5 left-6 overflow-clip rounded-xl top-38 w-85.5">
          <p className="absolute font-medium left-10.5 text-[#0680f9] text-[14px] top-4">내 사건과의 매칭 키워드</p>
          <p className="absolute font-normal left-4 text-[rgba(2,38,75,0.8)] text-[14px] top-11.75 w-79 leading-[22px]">사용자님의 사건 분석 결과, 다음 핵심 키워드에서 <strong>5건</strong>의 전문성이 확인되었습니다.</p>
          <div className="absolute flex flex-wrap gap-1 left-4 top-26.25 w-75">
            <span className="bg-[#0680f9] rounded-xl px-2 py-0.5 text-white text-[10px] font-medium">#임대차 계약</span>
            <span className="bg-[#0680f9] rounded-xl px-2 py-0.5 text-white text-[10px] font-medium">#권리금 반환</span>
            <span className="bg-[#0680f9] rounded-xl px-2 py-0.5 text-white text-[10px] font-medium">#상가 수선 의무</span>
            <span className="bg-[#0680f9] rounded-xl px-2 py-0.5 text-white text-[10px] font-medium">#부동산 분쟁</span>
            <span className="bg-[#0680f9] rounded-xl px-2 py-0.5 text-white text-[10px] font-medium">#내용증명</span>
          </div>
        </div>
        {/* 수행 사례 */}
        <div className="absolute bg-[#f9fafb] border border-[#e0e2e6] h-29.25 left-6 rounded-[10px] top-88.25 w-85.5">
          <p className="absolute font-medium left-10 text-[#16181d] text-[14px] top-3.75">주요 수행 사례</p>
          <p className="absolute font-bold left-4 text-[#16181d] text-[20px] top-13">120+</p>
          <p className="absolute font-normal left-4 text-[#575e6b] text-[10px] top-20.75">부동산 관련 승소</p>
          <p className="absolute font-bold left-44.5 text-[#16181d] text-[20px] top-13">15년</p>
          <p className="absolute font-normal left-44.5 text-[#575e6b] text-[10px] top-20.75">통합 실무 경력</p>
        </div>
        {/* 자격 및 약력 */}
        <p className="absolute font-bold left-12 text-[#575e6b] text-[14px] top-123.5 tracking-[0.7px] uppercase">전문 자격 및 약력</p>
        <div className="absolute left-6 top-135.5 w-85.5">
          <div className="flex items-start gap-2 mb-4 pb-4 border-b border-[#e0e2e6]">
            <div className="w-7.5"></div>
            <div><p className="font-medium text-[#16181d] text-[14px]">대한변호사협회 부동산 전문 변호사 등록</p><p className="font-normal text-[#575e6b] text-[12px]">대한변호사협회</p></div>
          </div>
          <div className="flex items-start gap-2 mb-4 pb-4 border-b border-[#e0e2e6]">
            <div className="w-7.5"></div>
            <div><p className="font-medium text-[#16181d] text-[14px]">서울지방변호사협회 건설법 연수 수료</p><p className="font-normal text-[#575e6b] text-[12px]">서울지방변호사협회</p></div>
          </div>
          <div className="flex items-start gap-2 mb-4 pb-4 border-b border-[#e0e2e6]">
            <div className="w-7.5"></div>
            <div><p className="font-medium text-[#16181d] text-[14px]">전국 임대차 분쟁 조정위원회 위원</p><p className="font-normal text-[#575e6b] text-[12px]">법무부</p></div>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-7.5"></div>
            <div><p className="font-medium text-[#16181d] text-[14px]">부동산 자산관리사(CPM) 자격 보유</p><p className="font-normal text-[#575e6b] text-[12px]">IREM</p></div>
          </div>
        </div>
        {/* 소개 */}
        <p className="absolute font-bold left-12 text-[#575e6b] text-[14px] top-199.25 tracking-[0.7px] uppercase">소개</p>
        <p className="absolute font-normal left-6 text-[#16181d] text-[14px] top-207.25 w-86 leading-[23px]">김성민 변호사는 지난 15년간 부동산 및 임대차 분쟁 해결에 집중해 왔습니다. 단순한 법률 자문을 넘어 의뢰인의 실질적인 권리 회복을 최우선으로 하며, AI 분석 시스템을 통한 정교한 증거 구조화 서비스를 지원합니다.</p>
      </div>
      {/* 헤더 */}
      <div className="absolute bg-white h-27 left-0 top-0 w-97.5">
        <div className="absolute h-0 left-0 top-27 w-97.5 border-b border-[#e0e2e6]"></div>
        <button className="absolute bg-transparent left-2 w-10 h-10 top-13.25">
          <div className="-translate-x-1/2 -translate-y-1/2 absolute left-1/2 w-6 h-6 top-1/2">
            <div className="absolute inset-[20.83%_33.33%]"><img alt="뒤로" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup14} /></div>
          </div>
        </button>
        <p className="absolute font-medium left-37.75 text-[#16181d] text-[16px] top-15.25">변호사 프로필</p>
      </div>
      {/* 하단 의뢰서 전달 버튼 */}
      <div className="absolute bg-white h-22.25 left-0 shadow-[0px_2px_4px_0px_rgba(23,25,28,0.06)] top-265.25 w-97.5">
        <div className="absolute h-0 left-0 top-0 w-97.5 border-t border-[#e0e2e6]"></div>
        <a className="absolute bg-[#0680f9] block h-14 left-4 overflow-clip rounded-[14px] shadow-[0px_2px_4px_0px_rgba(23,25,28,0.06)] top-4.25 w-89.5">
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[18px] text-white top-3.5">의뢰서 전달</p>
        </a>
      </div>
    </div>
  );
}
