// Figma Node: 1:1853 - Request Confirmation (390x844)
// Source: get_design_context from fileKey 6eFZg6uOGTZiiNZTA8YyGF
// Full raw JSX - modal overlay for confirming brief delivery to lawyer

const imgAvatar = "https://www.figma.com/api/mcp/asset/58b30aec-a259-47ad-afb3-b55c7c15a627";
const imgAvatar1 = "https://www.figma.com/api/mcp/asset/677d9957-1b8a-48ee-a1ed-5236e00a23cc";
const imgLine = "https://www.figma.com/api/mcp/asset/15aed072-71ed-49fa-bb66-aa916bda55a6";
const imgGroup = "https://www.figma.com/api/mcp/asset/36506275-77da-4382-a811-21a7e9c1bdc8";
const imgGroup17 = "https://www.figma.com/api/mcp/asset/0374f3e2-7f2c-45e5-bf94-99c3466a575b";
const imgGroup18 = "https://www.figma.com/api/mcp/asset/ce208754-b820-4e1a-9909-131bde44129e";
const imgGroup21 = "https://www.figma.com/api/mcp/asset/d33f8658-1d4b-466c-838c-24b8aa9c6b08";
const imgGroup22 = "https://www.figma.com/api/mcp/asset/b8b4ee0e-4856-40fe-a9e5-7d954d4780d2";
const imgGroup23 = "https://www.figma.com/api/mcp/asset/75e11df9-d800-48a4-b4f1-8718d13817c2";
const imgLine1 = "https://www.figma.com/api/mcp/asset/6152fc01-be60-4d61-adaa-110ac9bc7dff";

export default function RequestConfirmation() {
  return (
    <div className="bg-white relative shadow-[0px_3px_6px_0px_rgba(18,15,40,0.12)] size-full" data-node-id="1:1853" data-name="Request Confirmation">
      {/* 배경 (변호사 프로필 - 블러) */}
      <div className="absolute bg-white h-211 left-0 top-0 w-97.5">
        {/* 헤더 */}
        <div className="absolute bg-white h-27 left-0 top-0 w-97.5">
          <div className="absolute h-0 left-0 top-27 w-97.5 border-b border-[#e0e2e6]"></div>
          <button className="absolute bg-transparent left-2 w-10 h-10 top-13.25">
            <div className="-translate-x-1/2 -translate-y-1/2 absolute left-1/2 w-6 h-6 top-1/2">
              <div className="absolute inset-[20.83%_33.33%]"><img alt="뒤로" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup} /></div>
            </div>
          </button>
          <p className="absolute font-medium left-[137.5px] text-[#16181d] text-[16px] top-15">변호사 프로필</p>
        </div>
        {/* 블러 배경 콘텐츠 */}
        <div className="absolute h-184 left-0 opacity-40 top-27 w-97.5">
          <div className="absolute bg-[#e7fae4] left-36.25 overflow-clip rounded-[50px] size-25 top-8">
            <img alt="" className="absolute inset-0 w-full h-full object-cover" src={imgAvatar} />
          </div>
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[#16181d] text-[24px] text-center top-37">정윤석 변호사</p>
          <p className="absolute font-normal left-1/2 -translate-x-1/2 text-[#575e6b] text-[16px] text-center top-45">기업 법무 / 형사 전문</p>
          <div className="absolute bg-[#0680f9] h-14 left-6 rounded-[10px] top-152 w-85.5">
            <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[18px] text-white top-3.5">의뢰서 전달</p>
          </div>
        </div>
      </div>
      {/* 모달 오버레이 */}
      <div className="absolute bg-[rgba(0,0,0,0.63)] h-211 left-0 top-0 w-97.5">
        <div className="absolute bg-white border border-[#e0e2e6] h-130.25 left-[44.5px] rounded-[14px] shadow-[0px_6px_12px_0px_rgba(23,25,28,0.1)] top-[145.5px] w-75.75">
          {/* 아이콘 */}
          <div className="absolute bg-[rgba(6,128,249,0.1)] left-31.75 rounded-3xl size-12 top-5.5">
            <div className="absolute left-3 overflow-clip size-6 top-3">
              <div className="absolute inset-[4.17%_12.5%]"><img alt="" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup17} /></div>
            </div>
          </div>
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[#16181d] text-[18px] text-center top-20">의뢰서 전달 확인</p>
          <div className="absolute font-normal left-1/2 -translate-x-1/2 text-[#575e6b] text-[12px] text-center top-29.5 w-66">
            <p className="leading-[20px] mb-0">작성하신 사건 분석 리포트와 의뢰서를</p>
            <p className="leading-[20px]">선택하신 변호사에게 전달하시겠습니까?</p>
          </div>
          {/* 변호사 정보 카드 */}
          <div className="absolute bg-[#f9fafb] border border-[#e0e2e6] h-19.75 left-5.5 rounded-xl top-52.25 w-65">
            <div className="absolute bg-[#ece5fd] left-3.75 overflow-clip rounded-xl size-6.25 top-3.75">
              <img alt="" className="absolute h-[175%] left-0 w-full top-[-37.5%]" src={imgAvatar1} />
            </div>
            <p className="absolute font-bold left-14 text-[#16181d] text-[14px] top-4">정윤석 변호사</p>
            <span className="absolute bg-[rgba(6,128,249,0.1)] rounded-[10px] px-1 left-36.5 text-[#0680f9] text-[10px] top-4.5">전문</span>
            <p className="absolute font-normal left-14 text-[#575e6b] text-[11px] top-10.5">기업 법무 / 형사 전문 · 경력 15년</p>
          </div>
          {/* 주의사항 */}
          <div className="absolute bg-[rgba(230,77,77,0.05)] border border-[rgba(230,77,77,0.1)] h-19.75 left-5.5 rounded-xl top-76.5 w-65">
            <p className="absolute font-bold left-9.75 text-[#e64d4d] text-[12px] top-2.75">주의사항</p>
            <p className="absolute font-normal left-9.75 text-[rgba(230,77,77,0.8)] text-[10px] top-8 w-47.5 leading-[17px]">전달 후에는 의뢰 내용을 수정하거나 취소할 수 없습니다. 내용을 다시 한번 확인해 주세요.</p>
          </div>
          {/* 버튼 */}
          <a className="absolute bg-white border border-[#e0e2e6] block h-7.25 left-5.5 overflow-clip rounded-[10px] top-102 w-14.25">
            <p className="absolute font-medium left-1/2 -translate-x-1/2 text-[#16181d] text-[10px] text-center top-0.75">취소</p>
          </a>
          <a className="absolute bg-[#0680f9] block h-7.25 left-22.25 overflow-clip rounded-[10px] shadow-[0px_2px_4px_0px_rgba(23,25,28,0.06)] top-102 w-15.25">
            <p className="absolute font-medium left-1/2 -translate-x-1/2 text-[10px] text-white text-center top-0.75">전달하기</p>
          </a>
          {/* 하단 보안 안내 */}
          <div className="absolute bg-[rgba(243,244,246,0.3)] h-13.75 left-0 rounded-bl-[14px] rounded-br-[14px] top-116.25 w-75.5">
            <div className="absolute h-0 left-0 top-0 w-75 border-t border-[#e0e2e6]"></div>
            <p className="absolute font-medium left-16.5 text-[#575e6b] text-[10px] top-5.25 tracking-[0.5px] uppercase">Securely Handled by SHIELD AI</p>
          </div>
        </div>
      </div>
    </div>
  );
}
