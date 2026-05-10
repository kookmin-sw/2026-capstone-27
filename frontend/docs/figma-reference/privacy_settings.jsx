// Figma Node: 1:1393 - Privacy Settings (390x1213)
// Source: get_design_context from fileKey 6eFZg6uOGTZiiNZTA8YyGF

const imgAvatar = "https://www.figma.com/api/mcp/asset/fa7262be-8fe1-4507-847b-08ab3d59a0bd";
const imgGroup = "https://www.figma.com/api/mcp/asset/52c69d60-a27a-4c1b-a033-04e7a3fe90fe";
const imgGroup1 = "https://www.figma.com/api/mcp/asset/291d4bab-9851-4135-b6d2-0d7d6caf1691";
const imgGroup2 = "https://www.figma.com/api/mcp/asset/bcaea52b-680a-438c-8b47-8da31bd6fb91";
const imgGroup3 = "https://www.figma.com/api/mcp/asset/77e263b3-2385-4c34-bd39-b6a78e4943d0";
const imgGroup4 = "https://www.figma.com/api/mcp/asset/2f1201b1-09ec-4a7c-914b-b4bbda3b15b4";
const imgGroup5 = "https://www.figma.com/api/mcp/asset/79db20f8-ca9c-4cbf-9443-f51801ed5ed5";
const imgGroup6 = "https://www.figma.com/api/mcp/asset/5ef63169-2447-4772-ba90-4f1494450194";
const imgGroup7 = "https://www.figma.com/api/mcp/asset/1afaf69a-90c0-4b40-a5a6-40f835102d41";
const imgGroup8 = "https://www.figma.com/api/mcp/asset/fa44b7fe-4904-4c27-97bf-b12a0a6d0e92";
const imgGroup9 = "https://www.figma.com/api/mcp/asset/ab10fb9a-0674-4abd-8ea3-e50ebc7c897e";
const imgLine = "https://www.figma.com/api/mcp/asset/8b55d8bd-66ff-495a-9f09-5298c8707d30";
const imgGroup10 = "https://www.figma.com/api/mcp/asset/28bfa2f6-7592-4f4f-bff9-1ad555036a86";

export default function PrivacySettings() {
  return (
    <div className="bg-white relative shadow-[0px_3px_6px_0px_rgba(18,15,40,0.12)] size-full" data-node-id="1:1393" data-name="Privacy Settings">
      <div className="absolute bg-[rgba(0,0,0,0)] h-276.25 left-0 overflow-clip top-27 w-97.5" data-node-id="1:1394">
        <div className="absolute font-bold h-18.75 left-[24.5px] text-[#161a1d] text-[20px] top-[17.5px] w-72" data-node-id="1:1395">
          <p className="leading-[32px] mb-0">변호사에게 공개할</p>
          <p className="leading-[32px]">본인의 신원을 선택해 주세요</p>
        </div>
        <p className="absolute font-normal leading-[20px] left-6 text-[#31383f] text-[14px] top-20.5" data-node-id="1:1396">설정한 공개 범위는 변호사와의 상담 시에만 적용됩니다.</p>
        {/* 전체 공개 카드 */}
        <div className="absolute bg-[#f9fafb] border-2 border-[#dde0e4] h-61.5 left-6 rounded-xl top-33.5 w-85.5" data-node-id="1:1399">
          <div className="absolute bg-[#f3f5f6] left-5 rounded-[10px] size-9 top-9.25"></div>
          <p className="absolute font-bold left-17 text-[#161a1d] text-[16px] top-5">전체 공개</p>
          <p className="absolute font-normal left-17 text-[#31383f] text-[14px] top-11.25 w-54.5 leading-[23px]">본인의 실명으로 사건을 의뢰합니다. 변호사의 신뢰도가 높아집니다.</p>
          <p className="absolute font-medium left-6.75 text-[#31383f] text-[11px] top-32 tracking-[-0.55px] uppercase">변호사 확인 화면 미리보기</p>
          <div className="absolute bg-[rgba(243,245,246,0.3)] border border-[#dde0e4] h-18.5 left-5 rounded-[10px] top-36.75 w-74.5">
            <div className="absolute bg-[#e1f1fd] left-4 overflow-clip rounded-[20px] size-10 top-4"><img alt="" className="absolute inset-0 w-full h-full" src={imgAvatar} /></div>
            <p className="absolute font-bold left-17 text-[#31383f] text-[14px] top-4">홍길동</p>
            <span className="absolute border border-[rgba(31,177,249,0.4)] rounded-lg px-1 left-27.75 text-[#1fb1f9] text-[10px] top-4.5">의뢰인</span>
            <p className="absolute font-normal left-17 text-[#31383f] text-[11px] top-9.5">상담 대기 중 · 민사 사건</p>
          </div>
        </div>
        {/* 부분 공개 카드 (선택됨) */}
        <div className="absolute bg-white border-2 border-[#1fb1f9] h-61.5 left-6 rounded-xl shadow-[0px_4px_6px_0px_rgba(0,0,0,0.1)] top-98.75 w-85.5" data-node-id="1:1426">
          <div className="absolute bg-[#1fb1f9] left-5 rounded-[10px] size-9 top-9.25"></div>
          <p className="absolute font-bold left-17 text-[#161a1d] text-[16px] top-4.75">부분 공개</p>
          <p className="absolute font-normal left-17 text-[#31383f] text-[14px] top-11.25 w-54.5 leading-[23px]">이름의 일부를 마스킹 처리하여 공개합니다. 보안과 신뢰의 균형을 맞춥니다.</p>
          <p className="absolute font-medium left-6.5 text-[#31383f] text-[11px] top-32 tracking-[-0.55px] uppercase">변호사 확인 화면 미리보기</p>
          <div className="absolute bg-[#f0faff] border border-[rgba(31,177,249,0.3)] h-18.5 left-5 rounded-[10px] top-36.75 w-74.5">
            <div className="absolute bg-[#e1f1fd] left-4 overflow-clip rounded-[20px] size-10 top-4"><img alt="" className="absolute inset-0 w-full h-full" src={imgAvatar} /></div>
            <p className="absolute font-bold left-17 text-[#161a1d] text-[14px] top-4">홍○동</p>
            <span className="absolute border border-[rgba(31,177,249,0.4)] rounded-lg px-1 left-27.5 text-[#1fb1f9] text-[10px] top-4.5">의뢰인</span>
            <p className="absolute font-normal left-17 text-[#31383f] text-[11px] top-9.75">상담 대기 중 · 민사 사건</p>
          </div>
        </div>
        {/* 비공개 카드 */}
        <div className="absolute bg-[#f9fafb] border-2 border-[#dde0e4] h-61.5 left-6 rounded-xl top-164.25 w-85.5" data-node-id="1:1453">
          <div className="absolute bg-[#f3f5f6] left-5 rounded-[10px] size-9 top-9.25"></div>
          <p className="absolute font-bold left-17 text-[#161a1d] text-[16px] top-5">비공개 (익명)</p>
          <p className="absolute font-normal left-17 text-[#31383f] text-[14px] top-11.25 w-54.5 leading-[23px]">실명을 완전히 숨기고 익명으로 의뢰합니다. 개인정보를 최우선으로 보호합니다.</p>
          <p className="absolute font-medium left-7 text-[#31383f] text-[11px] top-31.75 tracking-[-0.55px] uppercase">변호사 확인 화면 미리보기</p>
          <div className="absolute bg-[rgba(243,245,246,0.3)] border border-[#dde0e4] h-18.5 left-5 rounded-[10px] top-36.75 w-74.5">
            <div className="absolute bg-[#e1f1fd] left-4 overflow-clip rounded-[20px] size-10 top-4"><img alt="" className="absolute inset-0 w-full h-full" src={imgAvatar} /></div>
            <p className="absolute font-bold left-17 text-[#31383f] text-[14px] top-4">익명의 의뢰인</p>
            <span className="absolute border border-[rgba(31,177,249,0.4)] rounded-lg px-1 left-38.25 text-[#1fb1f9] text-[10px] top-4.5">의뢰인</span>
            <p className="absolute font-normal left-17 text-[#31383f] text-[11px] top-9.5">상담 대기 중 · 민사 사건</p>
          </div>
        </div>
        {/* 안내 */}
        <div className="absolute bg-[rgba(243,245,246,0.4)] border border-[#dde0e4] border-dashed h-18.5 left-6 rounded-xl top-232.75 w-85.5">
          <p className="absolute font-normal left-3.75 text-[10px] text-[#31383f] top-3.75 w-80.75 leading-[20px]">• 개인정보 공개 설정은 의뢰서 제출 이후에는 변경이 불가능합니다.<br/>• 정확한 법률 상담을 위해 가능하면 부분 공개 이상의 설정을 권장합니다.</p>
        </div>
      </div>
      {/* 다음 버튼 */}
      <div className="absolute bg-gradient-to-t from-white h-30 left-0 to-transparent top-273.5 w-97.5">
        <div className="absolute bg-[#1fb1f9] h-14 left-6 rounded-xl shadow-[0px_10px_15px_0px_rgba(31,177,249,0.25)] top-10 w-85.5">
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[16px] text-white top-3.75">다음</p>
        </div>
      </div>
      {/* 헤더 */}
      <div className="absolute bg-white h-27 left-0 top-0 w-97.5">
        <div className="absolute h-0 left-0 top-27 w-97.5 border-b border-[#e0e2e6]"></div>
        <button className="absolute bg-transparent left-2 rounded-[20px] w-10 h-10 top-13.25">
          <div className="-translate-x-1/2 -translate-y-1/2 absolute left-1/2 w-6 h-6 top-1/2">
            <div className="absolute inset-[20.83%_33.33%]"><img alt="뒤로" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup10} /></div>
          </div>
        </button>
        <p className="absolute font-medium left-32 text-[#161a1d] text-[18px] top-16">개인정보 공개 설정</p>
      </div>
    </div>
  );
}
