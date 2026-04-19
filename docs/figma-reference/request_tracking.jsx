// Figma Node: 1:1981 - Request Tracking / 의뢰 현황 (390x1056)
// Source: get_design_context from fileKey 6eFZg6uOGTZiiNZTA8YyGF
// Key UI elements: lawyer card, timeline steps (전달됨→열람→검토 진행 중→최종 결과), info banner, bottom nav

const imgAvatar = "https://www.figma.com/api/mcp/asset/b7f4b8e8-14bf-416f-b4ef-5730efe34a37";

export default function RequestTracking() {
  return (
    <div className="bg-white relative shadow-[0px_3px_6px_0px_rgba(18,15,40,0.12)] size-full" data-node-id="1:1981" data-name="Request Tracking">
      {/* 헤더: 의뢰 현황 + 검색/알림 아이콘 */}
      <div className="absolute bg-white h-27 left-0 top-0 w-97.5">
        <div className="absolute h-0 left-0 top-27 w-97.5 border-b border-[#e0e2e6]"></div>
        <button className="absolute bg-transparent left-2 w-10 h-10 top-13.25"></button>
        <p className="absolute font-medium left-39.5 text-[#16181d] text-[16px] top-15.75">의뢰 현황</p>
      </div>
      {/* 변호사 카드 */}
      <div className="absolute bg-[#f9fafb] h-27 left-6 rounded-xl shadow-[0px_2px_4px_0px_rgba(23,25,28,0.06)] top-33 w-85.5">
        <div className="absolute bg-[#fcfbd0] left-[14.5px] overflow-clip rounded-[28px] size-14 top-[18.5px]">
          <img alt="변호사" className="absolute inset-0 w-full h-full object-cover" src={imgAvatar} />
        </div>
        <p className="absolute font-bold left-22 text-[#16181d] text-[16px] top-4">김태형 변호사</p>
        <span className="absolute bg-[#f3f4f6] rounded-[9px] px-2 left-64 text-[#1f2228] text-[10px] font-medium top-5.25">MATCHED</span>
        <p className="absolute font-normal left-22 text-[#575e6b] text-[13px] top-11.25">형사 / 기업법무 • 경력 12년</p>
      </div>
      {/* 실시간 의뢰 프로세스 */}
      <p className="absolute font-bold left-6 text-[12px] text-[rgba(87,94,107,0.8)] top-66 tracking-[0.6px] uppercase">실시간 의뢰 프로세스</p>
      {/* 타임라인 */}
      <div className="absolute left-6 top-79 w-85.5">
        {/* Step 1: 의뢰서 전달됨 ✅ */}
        <div className="relative pl-12 pb-6">
          <div className="absolute left-0 bg-[#0680f9] border-2 border-[#0680f9] rounded-2xl w-8 h-8"></div>
          <div className="absolute left-3.75 top-8 w-0.5 h-10 bg-[#0680f9]"></div>
          <p className="font-bold text-[#16181d] text-[16px]">의뢰서 전달됨</p>
          <span className="bg-[#f3f4f6] rounded-[11px] px-2 text-[#575e6b] text-[11px] ml-4">2024.05.22 14:30</span>
          <p className="font-normal text-[#575e6b] text-[14px] mt-1">변호사님께 의뢰서가 성공적으로 전달되었습니다.</p>
        </div>
        {/* Step 2: 의뢰서 열람 ✅ */}
        <div className="relative pl-12 pb-6">
          <div className="absolute left-0 bg-[#0680f9] border-2 border-[#0680f9] rounded-2xl w-8 h-8"></div>
          <div className="absolute left-3.75 top-8 w-0.5 h-10 bg-[#0680f9]"></div>
          <p className="font-bold text-[#16181d] text-[16px]">의뢰서 열람</p>
          <span className="bg-[#f3f4f6] rounded-[11px] px-2 text-[#575e6b] text-[11px] ml-4">2024.05.22 16:15</span>
          <p className="font-normal text-[#575e6b] text-[14px] mt-1">변호사님이 의뢰 내용을 확인 중입니다.</p>
        </div>
        {/* Step 3: 검토 진행 중 🔵 (현재) */}
        <div className="relative pl-12 pb-6">
          <div className="absolute left-0 bg-white border-2 border-[#0680f9] rounded-2xl w-8 h-8"></div>
          <div className="absolute left-3.75 top-8 w-0.5 h-10 bg-[#f3f4f6]"></div>
          <p className="font-bold text-[#16181d] text-[16px]">검토 진행 중</p>
          <p className="font-normal text-[#575e6b] text-[14px] mt-1">수락 여부를 결정하고 있습니다.</p>
        </div>
        {/* Step 4: 최종 결과 ⚪ (대기) */}
        <div className="relative pl-12">
          <div className="absolute left-0 bg-white border-2 border-[#f3f4f6] rounded-2xl w-8 h-8"></div>
          <p className="font-bold text-[#575e6b] text-[16px]">최종 결과 (수락/거절)</p>
          <p className="font-normal text-[rgba(87,94,107,0.6)] text-[14px] mt-1">변호사님의 최종 답변이 도착할 예정입니다.</p>
        </div>
      </div>
      {/* 의뢰 응답 안내 */}
      <div className="absolute bg-[#f0f7ff] border border-[rgba(6,128,249,0.1)] h-28 left-6 rounded-[14px] top-165.75 w-85.5">
        <p className="absolute font-bold left-12 text-[#02264b] text-[14px] top-4">의뢰 응답 안내</p>
        <p className="absolute font-normal left-12 text-[rgba(2,38,75,0.8)] text-[12px] top-10 w-70.5 leading-[22px]">변호사가 의뢰를 수신한 후 <strong className="text-[#0680f9]">24시간 이내</strong>에 응답하지 않을 경우, 의뢰는 자동으로 거절 처리됩니다.</p>
      </div>
      {/* 의뢰 취소 버튼 */}
      <div className="absolute bg-[#f3f4f6] h-13 left-6 overflow-clip rounded-xl top-203.5 w-85.5">
        <p className="absolute font-medium left-1/2 -translate-x-1/2 text-[#1f2228] text-[14px] top-3.75">의뢰 취소하기</p>
      </div>
      <p className="absolute font-normal left-1/2 -translate-x-1/2 text-[#575e6b] text-[12px] text-center top-219.75">문제가 발생했나요? <span className="underline">고객센터 문의</span></p>
      {/* 하단 탭바 */}
      <div className="absolute bg-white h-16.25 left-0 shadow-[0px_-2px_10px_0px_rgba(0,0,0,0.05)] top-247.75 w-97.5">
        <div className="absolute h-0 left-0 top-0 w-97.5 border-t border-[#e0e2e6]"></div>
        <div className="flex h-16 top-0.25">
          <div className="flex-1 flex flex-col items-center justify-center"><p className="text-[#575e6b] text-[10px]">검색</p></div>
          <div className="flex-1 flex flex-col items-center justify-center"><p className="font-bold text-[#0680f9] text-[10px]">의뢰현황</p></div>
          <div className="flex-1 flex flex-col items-center justify-center"><p className="text-[#575e6b] text-[10px]">내 정보</p></div>
        </div>
      </div>
    </div>
  );
}
