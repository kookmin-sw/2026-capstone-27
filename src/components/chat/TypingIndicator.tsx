import { cn } from '@/lib/cn';

export function TypingIndicator() {
  return (
    <div className="flex flex-col items-start px-4 py-1">
      {/* Keyframe styles injected once */}
      <style>{`
        @keyframes chat-bounce {
          0%, 80%, 100% { transform: translateY(0); }
          40%            { transform: translateY(-6px); }
        }
      `}</style>

      <div
        className={cn(
          'inline-flex items-center gap-1.5 px-4 py-3',
          'bg-white border border-gray-200',
          'rounded-2xl rounded-bl-sm',
        )}
        aria-label="AI가 응답 중입니다"
        role="status"
      >
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="h-2 w-2 rounded-full bg-gray-400"
            style={{
              animation: `chat-bounce 1.2s ease-in-out ${i * 0.2}s infinite`,
            }}
          />
        ))}
      </div>

      <span className="mt-1 text-xs text-gray-400">AI 응답 중...</span>
    </div>
  );
}
