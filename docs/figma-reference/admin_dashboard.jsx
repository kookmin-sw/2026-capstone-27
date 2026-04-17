// Figma Node: 1:2580 - Admin Dashboard (390x697)

const imgEllipse = "https://www.figma.com/api/mcp/asset/18cb3b81-c9bd-4cc1-a0b3-4190f3c3c90e";
const imgGroup = "https://www.figma.com/api/mcp/asset/d365adf3-d08d-4719-a5c1-f7d53a06d471";
const imgGroup1 = "https://www.figma.com/api/mcp/asset/be708aef-2645-4c24-a465-d0492631c303";
const imgEllipse1 = "https://www.figma.com/api/mcp/asset/a1ea6541-ac11-4eee-892b-348bb63a9ff6";

export default function AdminDashboard() {
  return (
    <div className="bg-[#f7f8fa] relative size-full" data-node-id="1:2580" data-name="Admin - Dashboard">
      <div className="absolute bg-white h-[48px] left-0 overflow-clip top-[40px] w-[390px]" data-node-id="1:2581" data-name="Frame">
        <div className="absolute bg-[#e9edef] h-[0.5px] left-0 top-[47px] w-[390px]" data-node-id="1:2582" data-name="Rectangle" />
        <div className="absolute left-[12px] size-[28px] top-[10px]" data-node-id="1:2583" data-name="Ellipse">
          <img alt="" className="absolute block inset-0 max-w-none size-full" src={imgEllipse} />
        </div>
        <p className="absolute font-['Inter:Regular','Noto_Sans:Regular',sans-serif] font-normal leading-[normal] left-[21px] not-italic text-[14px] text-white top-[14px] whitespace-nowrap" data-node-id="1:2584">
          ⊞
        </p>
        <p className="-translate-x-1/2 absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium h-[20px] leading-[normal] left-[195px] not-italic text-[#1a1a1a] text-[15px] text-center top-[14px] w-[390px]" data-node-id="1:2585">
          관리자 콘솔
        </p>
      </div>
      <div className="absolute bg-[rgba(0,0,0,0)] h-[40px] left-0 top-0 w-[390px]" data-node-id="1:2586" data-name="Container">
        <div className="absolute h-[40px] left-0 overflow-clip top-0 w-[70px]" data-node-id="1:2587" data-name="Image">
          <div className="absolute inset-[41.96%_19.41%_31.28%_41.5%]" data-node-id="1:2588" data-name="Group">
            <img alt="" className="absolute block inset-0 max-w-none size-full" src={imgGroup} />
          </div>
        </div>
        <div className="absolute h-[40px] left-[294px] overflow-clip top-0 w-[96px]" data-node-id="1:2593" data-name="Image">
          <div className="absolute inset-[41.68%_18.88%_31.92%_12.5%]" data-node-id="1:2594" data-name="Group">
            <div className="absolute inset-[-4.73%_0_0_0]">
              <img alt="" className="block max-w-none size-full" src={imgGroup1} />
            </div>
          </div>
        </div>
      </div>
      <div className="absolute bg-[#1a6de0] h-[76px] leading-[normal] left-[14px] not-italic overflow-clip rounded-[14px] top-[104px] w-[176px] whitespace-nowrap" data-node-id="1:2603" data-name="Frame">
        <p className="absolute font-['Inter:Medium',sans-serif] font-medium left-[14px] text-[28px] text-white top-[10px]" data-node-id="1:2604">
          18
        </p>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal left-[14px] text-[11px] text-[rgba(255,255,255,0.75)] top-[46px]" data-node-id="1:2605">
          승인 대기
        </p>
      </div>
      <div className="absolute bg-white border-[#e9edef] border-[0.5px] border-solid h-[76px] leading-[normal] left-[200px] not-italic overflow-clip rounded-[14px] top-[104px] w-[176px] whitespace-nowrap" data-node-id="1:2606" data-name="Frame">
        <p className="absolute font-['Inter:Medium',sans-serif] font-medium left-[13.5px] text-[#1a6de0] text-[28px] top-[9.5px]" data-node-id="1:2607">
          5
        </p>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal left-[13.5px] text-[#6b7280] text-[11px] top-[45.5px]" data-node-id="1:2608">
          검토 중
        </p>
      </div>
      <div className="absolute bg-white border-[#e9edef] border-[0.5px] border-solid h-[76px] leading-[normal] left-[14px] not-italic overflow-clip rounded-[14px] top-[194px] w-[176px] whitespace-nowrap" data-node-id="1:2609" data-name="Frame">
        <p className="absolute font-['Inter:Medium',sans-serif] font-medium left-[13.5px] text-[#854f0b] text-[28px] top-[9.5px]" data-node-id="1:2610">
          3
        </p>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal left-[13.5px] text-[#6b7280] text-[11px] top-[45.5px]" data-node-id="1:2611">
          보완 요청
        </p>
      </div>
      <div className="absolute bg-white border-[#e9edef] border-[0.5px] border-solid h-[76px] leading-[normal] left-[200px] not-italic overflow-clip rounded-[14px] top-[194px] w-[176px] whitespace-nowrap" data-node-id="1:2612" data-name="Frame">
        <p className="absolute font-['Inter:Medium',sans-serif] font-medium left-[13.5px] text-[#3b6e11] text-[28px] top-[9.5px]" data-node-id="1:2613">
          7
        </p>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal left-[13.5px] text-[#6b7280] text-[11px] top-[45.5px]" data-node-id="1:2614">
          오늘 처리
        </p>
      </div>
      <div className="absolute bg-[#fff8e5] border-[#f0c06f] border-[0.5px] border-solid h-[80px] leading-[normal] left-[14px] not-italic overflow-clip rounded-[14px] text-[#854f0b] top-[298px] w-[362px]" data-node-id="1:2615" data-name="Frame">
        <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium left-[11.5px] text-[12px] top-[9.5px] whitespace-nowrap" data-node-id="1:2616">
          ⚠ 빠른 확인 필요
        </p>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal h-[20px] left-[11.5px] text-[11px] top-[31.5px] w-[340px] whitespace-pre-wrap" data-node-id="1:2617">{`• 24시간 이상 미처리 4건    • 서류 누락 2건    • 중복 의심 1건`}</p>
      </div>
      <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[14px] not-italic text-[#1a1a1a] text-[14px] top-[395px] whitespace-nowrap" data-node-id="1:2618">
        최근 신청
      </p>
      <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[310px] not-italic text-[#1a6de0] text-[12px] top-[397px] whitespace-nowrap" data-node-id="1:2619">
        전체 보기 →
      </p>
      <div className="absolute bg-white border-[#e9edef] border-[0.5px] border-solid h-[96px] left-[14px] overflow-clip rounded-[14px] top-[424px] w-[362px]" data-node-id="1:2620" data-name="Frame">
        <div className="absolute left-[11.5px] size-[36px] top-[11.5px]" data-node-id="1:2621" data-name="Ellipse">
          <img alt="" className="absolute block inset-0 max-w-none size-full" src={imgEllipse1} />
        </div>
        <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[23.5px] not-italic text-[#1a6de0] text-[14px] top-[20.5px] whitespace-nowrap" data-node-id="1:2622">
          홍
        </p>
        <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[55.5px] not-italic text-[#1a1a1a] text-[14px] top-[11.5px] whitespace-nowrap" data-node-id="1:2623">
          홍길동
        </p>
        <p className="absolute font-['Inter:Regular',sans-serif] font-normal leading-[normal] left-[55.5px] not-italic text-[#adb5b8] text-[11px] top-[31.5px] whitespace-nowrap" data-node-id="1:2624">
          lawyer@shield.com
        </p>
        <div className="absolute contents left-[284.5px] top-[11.5px]" data-node-id="1:2625">
          <div className="absolute bg-[#f1f0e8] h-[22px] left-[285px] rounded-[11px] top-[12px] w-[70px]" data-node-id="1:2626" data-name="Rectangle" />
          <p className="-translate-x-1/2 absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium h-[20px] leading-[normal] left-[320px] not-italic text-[#5f5e5a] text-[11px] text-center top-[16px] w-[58px]" data-node-id="1:2627">
            승인 대기
          </p>
        </div>
        <div className="absolute contents left-[11.5px] top-[52.5px]" data-node-id="1:2628">
          <div className="absolute bg-[#e8f0fc] h-[20px] left-[12px] rounded-[10px] top-[53px] w-[37px]" data-node-id="1:2629" data-name="Rectangle" />
          <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[17px] not-italic text-[#0c447c] text-[10px] top-[57px] whitespace-nowrap" data-node-id="1:2630">
            임대차
          </p>
        </div>
        <div className="absolute contents left-[56.5px] top-[52.5px]" data-node-id="1:2631">
          <div className="absolute bg-[#e8f0fc] h-[20px] left-[57px] rounded-[10px] top-[53px] w-[37px]" data-node-id="1:2632" data-name="Rectangle" />
          <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[62px] not-italic text-[#0c447c] text-[10px] top-[57px] whitespace-nowrap" data-node-id="1:2633">
            부동산
          </p>
        </div>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[11.5px] not-italic text-[#6b7280] text-[10px] top-[77.5px] whitespace-nowrap" data-node-id="1:2634">
          경력 8년 · 04.08
        </p>
        <div className="absolute contents left-[284.5px] top-[66.5px]" data-node-id="1:2635">
          <div className="absolute bg-[#1a6de0] h-[22px] left-[285px] rounded-[11px] top-[67px] w-[68px]" data-node-id="1:2636" data-name="Rectangle" />
          <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[297px] not-italic text-[11px] text-white top-[71px] whitespace-nowrap" data-node-id="1:2637">
            상세 보기
          </p>
        </div>
      </div>
      <div className="absolute bg-white border-[#e9edef] border-[0.5px] border-solid h-[96px] left-[14px] overflow-clip rounded-[14px] top-[534px] w-[362px]" data-node-id="1:2638" data-name="Frame">
        <div className="absolute left-[11.5px] size-[36px] top-[11.5px]" data-node-id="1:2639" data-name="Ellipse">
          <img alt="" className="absolute block inset-0 max-w-none size-full" src={imgEllipse1} />
        </div>
        <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[22.5px] not-italic text-[#1a6de0] text-[14px] top-[19.5px] whitespace-nowrap" data-node-id="1:2640">
          김
        </p>
        <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[55.5px] not-italic text-[#1a1a1a] text-[14px] top-[11.5px] whitespace-nowrap" data-node-id="1:2641">
          김민지
        </p>
        <p className="absolute font-['Inter:Regular',sans-serif] font-normal leading-[normal] left-[55.5px] not-italic text-[#adb5b8] text-[11px] top-[31.5px] whitespace-nowrap" data-node-id="1:2642">
          kimj@shield.com
        </p>
        <div className="absolute contents left-[284.5px] top-[11.5px]" data-node-id="1:2643">
          <div className="absolute bg-[#e8f0fc] h-[22px] left-[285px] rounded-[11px] top-[12px] w-[70px]" data-node-id="1:2644" data-name="Rectangle" />
          <p className="-translate-x-1/2 absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium h-[20px] leading-[normal] left-[320px] not-italic text-[#0c5fa5] text-[11px] text-center top-[16px] w-[58px]" data-node-id="1:2645">
            검토 중
          </p>
        </div>
        <div className="absolute contents left-[11.5px] top-[52.5px]" data-node-id="1:2646">
          <div className="absolute bg-[#e8f0fc] h-[20px] left-[12px] rounded-[10px] top-[53px] w-[30px]" data-node-id="1:2647" data-name="Rectangle" />
          <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[18px] not-italic text-[#0c447c] text-[10px] top-[57px] whitespace-nowrap" data-node-id="1:2648">
            형사
          </p>
        </div>
        <p className="absolute font-['Inter:Regular','Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] left-[11.5px] not-italic text-[#6b7280] text-[10px] top-[77.5px] whitespace-nowrap" data-node-id="1:2649">
          경력 3년 · 04.08
        </p>
        <div className="absolute contents left-[285.5px] top-[66.5px]" data-node-id="1:2650">
          <div className="absolute bg-[#1a6de0] h-[22px] left-[286px] rounded-[11px] top-[67px] w-[68px]" data-node-id="1:2651" data-name="Rectangle" />
          <p className="absolute font-['Inter:Medium','Noto_Sans_KR:Regular',sans-serif] font-medium leading-[normal] left-[298px] not-italic text-[11px] text-white top-[71px] whitespace-nowrap" data-node-id="1:2652">
            상세 보기
          </p>
        </div>
      </div>
    </div>
  );
}
