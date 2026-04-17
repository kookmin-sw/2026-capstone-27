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
    <div className="flex flex-col min-h-dvh bg-white items-center justify-center relative overflow-hidden animate-[fadeIn_0.6s_ease-out_both]">
      {/* Decorative blur */}
      <div className="absolute -top-20 right-0 w-64 h-64 rounded-full bg-brand/5 blur-[64px]" />

      {/* Shield logo — large centered icon only */}
      <div className="w-[170px] h-[170px] rounded-[32px] bg-brand flex items-center justify-center shadow-lg shadow-brand/20">
        <Shield size={96} className="text-white" strokeWidth={1.5} aria-hidden="true" />
      </div>

      {/* Loading dots */}
      <div className="flex items-center gap-1.5 mt-10" aria-label="로딩 중">
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
