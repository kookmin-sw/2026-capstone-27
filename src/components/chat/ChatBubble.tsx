import { Lightbulb } from 'lucide-react';
import { cn } from '@/lib/cn';

type Sender = 'USER' | 'CHATBOT' | 'CHATBOT_TIP' | 'ROUTER_REQUEST' | 'SYSTEM';

interface ChatBubbleProps {
  sender: Sender;
  content: string;
  timestamp: string;
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp);
  if (isNaN(date.getTime())) return timestamp;
  return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
}

export function ChatBubble({ sender, content, timestamp }: ChatBubbleProps) {
  const formattedTime = formatTime(timestamp);

  if (sender === 'SYSTEM') {
    return (
      <div className="flex justify-center px-4 py-1">
        <div className="w-full max-w-full bg-red-50 text-red-600 rounded-lg px-3 py-2 text-center text-xs leading-snug">
          {content}
        </div>
        <span className="sr-only">{formattedTime}</span>
      </div>
    );
  }

  if (sender === 'USER') {
    return (
      <div className="flex flex-col items-end px-4 py-1">
        <div
          className={cn(
            'max-w-[75%] px-4 py-2.5',
            'bg-brand text-white',
            'rounded-2xl rounded-br-sm',
            'text-sm leading-relaxed break-words',
          )}
        >
          {content}
        </div>
        <span className="mt-1 text-xs text-gray-400">{formattedTime}</span>
      </div>
    );
  }

  if (sender === 'CHATBOT_TIP') {
    return (
      <div className="flex flex-col items-start px-4 py-1">
        <div
          className={cn(
            'max-w-[75%] px-4 py-2.5',
            'bg-[var(--color-info-bg)] text-blue-700',
            'rounded-2xl rounded-bl-sm',
            'text-sm leading-relaxed break-words',
            'flex items-start gap-2',
          )}
        >
          <Lightbulb className="mt-0.5 h-4 w-4 shrink-0 text-blue-500" />
          <span>{content}</span>
        </div>
        <span className="mt-1 text-xs text-gray-400">{formattedTime}</span>
      </div>
    );
  }

  if (sender === 'ROUTER_REQUEST') {
    return (
      <div className="flex flex-col items-start px-4 py-1">
        <div className="flex items-center gap-2 mb-1">
          <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 text-[11px] font-medium leading-none">
            분류 요청
          </span>
        </div>
        <div
          className={cn(
            'max-w-[75%] px-4 py-2.5',
            'bg-white text-gray-900 border border-gray-200',
            'rounded-2xl rounded-bl-sm',
            'text-sm leading-relaxed break-words',
          )}
        >
          {content}
        </div>
        <span className="mt-1 text-xs text-gray-400">{formattedTime}</span>
      </div>
    );
  }

  // CHATBOT (default)
  return (
    <div className="flex flex-col items-start px-4 py-1">
      <div
        className={cn(
          'max-w-[75%] px-4 py-2.5',
          'bg-white text-gray-900 border border-gray-200',
          'rounded-2xl rounded-bl-sm',
          'text-sm leading-relaxed break-words',
        )}
      >
        {content}
      </div>
      <span className="mt-1 text-xs text-gray-400">{formattedTime}</span>
    </div>
  );
}
