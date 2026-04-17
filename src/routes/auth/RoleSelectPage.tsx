import { ArrowLeft, Briefcase, ChevronRight, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/cn';

interface RoleCardProps {
  icon: React.ReactNode;
  iconBg: string;
  title: string;
  description: string;
  onClick: () => void;
}

function RoleCard({ icon, iconBg, title, description, onClick }: RoleCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'w-full text-left relative',
        'bg-white rounded-card',
        'hover:shadow-md transition-all duration-200',
        'h-32.5 px-6 cursor-pointer',
        'border-2 border-[#e0e2e6] hover:border-brand',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
      )}
    >
      <div className="flex items-center gap-4">
        <div className={cn('shrink-0 flex items-center justify-center w-14 h-14 rounded-full', iconBg)}>
          {icon}
        </div>
        <div className="flex flex-col gap-1 flex-1">
          <span className="text-lg font-bold text-[#16181d]">{title}</span>
          <span className="text-sm text-[#575e6b] leading-5.75">{description}</span>
        </div>
        <ChevronRight size={20} className="text-[#575e6b] opacity-60 shrink-0" />
      </div>
    </button>
  );
}

export function RoleSelectPage() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col min-h-dvh bg-white">
      {/* Header with back button */}
      <div className="sticky top-0 z-10 bg-white border-b border-[#e0e2e6] px-2 pt-2 pb-3">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className={cn(
            'flex items-center justify-center w-10 h-10',
            'text-[#16181d] hover:bg-gray-100 rounded-full',
            'transition-colors duration-150',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
          )}
          aria-label="뒤로 가기"
        >
          <ArrowLeft size={24} />
        </button>
      </div>

      <div className="flex-1 px-6 pt-8 pb-6 flex flex-col">
        {/* Title — left aligned, two lines */}
        <h1 className="text-2xl font-bold text-[#16181d] tracking-tight leading-8 mb-2">
          어떤 역할로
          <br />
          이용하시겠습니까?
        </h1>
        <p className="text-sm text-[#575e6b] mb-10">
          회원님의 이용 목적에 맞는 역할을 선택해 주세요.
        </p>

        {/* Role cards */}
        <div className="flex flex-col gap-4.75">
          <RoleCard
            icon={<User size={28} className="text-brand" />}
            iconBg="bg-brand/10"
            title="의뢰인 (Client)"
            description="인공지능 법률 상담을 통해 고민을 해결하고 적합한 변호사를 찾고 싶습니다."
            onClick={() => navigate('/register/client')}
          />
          <RoleCard
            icon={<Briefcase size={28} className="text-green-500" />}
            iconBg="bg-green-500/10"
            title="변호사 (Lawyer)"
            description="전문적인 법률 지식을 공유하고 새로운 의뢰인과 사건을 수임하고 싶습니다."
            onClick={() => navigate('/register/lawyer')}
          />
        </div>

        {/* Secure text */}
        <p className="mt-auto pt-10 text-center text-xs text-[#575e6b] opacity-60 leading-relaxed">
          SHIELD의 모든 데이터는 강력한 보안 기술로 보호됩니다
        </p>
      </div>
    </div>
  );
}
