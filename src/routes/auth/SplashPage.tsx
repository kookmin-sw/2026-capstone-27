import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield } from 'lucide-react';

// ─── page ────────────────────────────────────────────────────────────────────

export function SplashPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setTimeout(() => {
      navigate('/login', { replace: true });
    }, 2000);

    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="flex flex-col min-h-dvh bg-white items-center justify-center gap-6 animate-[fadeIn_0.6s_ease-out_both]">
      {/* Logo */}
      <div className="w-20 h-20 rounded-2xl bg-brand flex items-center justify-center shadow-lg shadow-brand/30">
        <Shield size={42} className="text-white" strokeWidth={1.8} aria-hidden="true" />
      </div>

      {/* App name + tagline */}
      <div className="text-center space-y-1.5">
        <h1 className="text-3xl font-bold text-gray-900 tracking-tight">SHIELD</h1>
        <p className="text-sm text-gray-500">AI 법률 정보 구조화 플랫폼</p>
      </div>

      {/* Loading dots */}
      <div className="flex items-center gap-1.5 mt-2" aria-label="로딩 중">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="w-1.5 h-1.5 rounded-full bg-brand"
            style={{
              animation: `bounce 1s ease-in-out ${i * 0.15}s infinite`,
            }}
          />
        ))}
      </div>

      {/* Inline keyframes */}
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes bounce {
          0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
          40%            { transform: translateY(-6px); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
