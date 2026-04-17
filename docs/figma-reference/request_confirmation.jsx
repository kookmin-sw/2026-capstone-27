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
      <div className="absolute bg-white h-[844px] left-0 top-0 w-[390px]">
        {/* 헤더 */}
        <div className="absolute bg-white h-[108px] left-0 top-0 w-[390px]">
          <div className="absolute h-0 left-0 top-[108px] w-[390px] border-b border-[#e0e2e6]"></div>
          <button className="absolute bg-transparent left-[8px] w-[40px] h-[40px] top-[53px]">
            <div className="-translate-x-1/2 -translate-y-1/2 absolute left-1/2 w-[24px] h-[24px] top-1/2">
              <div className="absolute inset-[20.83%_33.33%]"><img alt="뒤로" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup} /></div>
            </div>
          </button>
          <p className="absolute font-medium left-[137.5px] text-[#16181d] text-[16px] top-[60px]">변호사 프로필</p>
        </div>
        {/* 블러 배경 콘텐츠 */}
        <div className="absolute h-[736px] left-0 opacity-40 top-[108px] w-[390px]">
          <div className="absolute bg-[#e7fae4] left-[145px] overflow-clip rounded-[50px] size-[100px] top-[32px]">
            <img alt="" className="absolute inset-0 w-full h-full object-cover" src={imgAvatar} />
          </div>
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[#16181d] text-[24px] text-center top-[148px]">정윤석 변호사</p>
          <p className="absolute font-normal left-1/2 -translate-x-1/2 text-[#575e6b] text-[16px] text-center top-[180px]">기업 법무 / 형사 전문</p>
          <div className="absolute bg-[#0680f9] h-[56px] left-[24px] rounded-[10px] top-[608px] w-[342px]">
            <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[18px] text-white top-[14px]">의뢰서 전달</p>
          </div>
        </div>
      </div>
      {/* 모달 오버레이 */}
      <div className="absolute bg-[rgba(0,0,0,0.63)] h-[844px] left-0 top-0 w-[390px]">
        <div className="absolute bg-white border border-[#e0e2e6] h-[521px] left-[44.5px] rounded-[14px] shadow-[0px_6px_12px_0px_rgba(23,25,28,0.1)] top-[145.5px] w-[303px]">
          {/* 아이콘 */}
          <div className="absolute bg-[rgba(6,128,249,0.1)] left-[127px] rounded-[24px] size-[48px] top-[22px]">
            <div className="absolute left-[12px] overflow-clip size-[24px] top-[12px]">
              <div className="absolute inset-[4.17%_12.5%]"><img alt="" className="absolute block inset-0 max-w-none w-full h-full" src={imgGroup17} /></div>
            </div>
          </div>
          <p className="absolute font-bold left-1/2 -translate-x-1/2 text-[#16181d] text-[18px] text-center top-[80px]">의뢰서 전달 확인</p>
          <div className="absolute font-normal left-1/2 -translate-x-1/2 text-[#575e6b] text-[12px] text-center top-[118px] w-[264px]">
            <p className="leading-[20px] mb-0">작성하신 사건 분석 리포트와 의뢰서를</p>
            <p className="leading-[20px]">선택하신 변호사에게 전달하시겠습니까?</p>
          </div>
          {/* 변호사 정보 카드 */}
          <div className="absolute bg-[#f9fafb] border border-[#e0e2e6] h-[79px] left-[22px] rounded-[12px] top-[209px] w-[260px]">
            <div className="absolute bg-[#ece5fd] left-[15px] overflow-clip rounded-[12px] size-[25px] top-[15px]">
              <img alt="" className="absolute h-[175%] left-0 w-full top-[-37.5%]" src={imgAvatar1} />
            </div>
            <p className="absolute font-bold left-[56px] text-[#16181d] text-[14px] top-[16px]">정윤석 변호사</p>
            <span className="absolute bg-[rgba(6,128,249,0.1)] rounded-[10px] px-1 left-[146px] text-[#0680f9] text-[10px] top-[18px]">전문</span>
            <p className="absolute font-normal left-[56px] text-[#575e6b] text-[11px] top-[42px]">기업 법무 / 형사 전문 · 경력 15년</p>
          </div>
          {/* 주의사항 */}
          <div className="absolute bg-[rgba(230,77,77,0.05)] border border-[rgba(230,77,77,0.1)] h-[79px] left-[22px] rounded-[12px] top-[306px] w-[260px]">
            <p className="absolute font-bold left-[39px] text-[#e64d4d] text-[12px] top-[11px]">주의사항</p>
            <p className="absolute font-normal left-[39px] text-[rgba(230,77,77,0.8)] text-[10px] top-[32px] w-[190px] leading-[17px]">전달 후에는 의뢰 내용을 수정하거나 취소할 수 없습니다. 내용을 다시 한번 확인해 주세요.</p>
          </div>
          {/* 버튼 */}
          <a className="absolute bg-white border border-[#e0e2e6] block h-[29px] left-[22px] overflow-clip rounded-[10px] top-[408px] w-[57px]">
            <p className="absolute font-medium left-1/2 -translate-x-1/2 text-[#16181d] text-[10px] text-center top-[3px]">취소</p>
          </a>
          <a className="absolute bg-[#0680f9] block h-[29px] left-[89px] overflow-clip rounded-[10px] shadow-[0px_2px_4px_0px_rgba(23,25,28,0.06)] top-[408px] w-[61px]">
            <p className="absolute font-medium left-1/2 -translate-x-1/2 text-[10px] text-white text-center top-[3px]">전달하기</p>
          </a>
          {/* 하단 보안 안내 */}
          <div className="absolute bg-[rgba(243,244,246,0.3)] h-[55px] left-0 rounded-bl-[14px] rounded-br-[14px] top-[465px] w-[302px]">
            <div className="absolute h-0 left-0 top-0 w-[300px] border-t border-[#e0e2e6]"></div>
            <p className="absolute font-medium left-[66px] text-[#575e6b] text-[10px] top-[21px] tracking-[0.5px] uppercase">Securely Handled by SHIELD AI</p>
          </div>
        </div>
      </div>
    </div>
  );
}
