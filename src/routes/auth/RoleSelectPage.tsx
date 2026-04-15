import { ArrowLeft, Briefcase, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/cn';

interface RoleCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  onClick: () => void;
}

function RoleCard({ icon, title, description, onClick }: RoleCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'w-full text-left',
        'bg-white rounded-card shadow-sm',
        'hover:shadow-md transition-all duration-200',
        'p-6 cursor-pointer',
        'border-2 border-transparent hover:border-brand',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
      )}
    >
      <div className="flex items-start gap-4">
        <div className="flex-shrink-0 flex items-center justify-center w-12 h-12 rounded-xl bg-blue-50 text-brand">
          {icon}
        </div>
        <div className="flex flex-col gap-1">
          <span className="text-base font-semibold text-[#1E293B]">{title}</span>
          <span className="text-sm text-[#64748B] leading-relaxed">{description}</span>
        </div>
      </div>
    </button>
  );
}

export function RoleSelectPage() {
  const navigate = useNavigate();

  return (
    <div className="px-4 py-6 max-w-md mx-auto flex flex-col min-h-dvh">
      {/* Back button */}
      <button
        type="button"
        onClick={() => navigate(-1)}
        className={cn(
          'self-start flex items-center gap-1.5 text-sm text-[#64748B]',
          'hover:text-[#1E293B] transition-colors duration-150',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 rounded-md',
          'mb-8',
        )}
        aria-label="뒤로 가기"
      >
        <ArrowLeft size={18} />
        <span>뒤로</span>
      </button>

      {/* Title */}
      <h1 className="text-2xl font-bold text-[#1E293B] text-center mb-8 leading-tight">
        어떤 역할로 이용하시겠습니까?
      </h1>

      {/* Role cards */}
      <div className="flex flex-col gap-4">
        <RoleCard
          icon={<User size={24} />}
          title="의뢰인"
          description="인공지능 법률 상담을 통해 고민을 해결하고 적합한 변호사를 찾고 싶습니다"
          onClick={() => navigate('/register/client')}
        />
        <RoleCard
          icon={<Briefcase size={24} />}
          title="변호사"
          description="전문적인 법률 지식을 공유하고 새로운 의뢰인과 사건을 수임하고 싶습니다"
          onClick={() => navigate('/register/lawyer')}
        />
      </div>

      {/* Secure text */}
      <p className="mt-auto pt-10 text-center text-xs text-[#94A3B8] leading-relaxed">
        🔒 SHIELD의 모든 데이터는 강력한 보안 기술로 보호됩니다
      </p>
    </div>
  );
}
