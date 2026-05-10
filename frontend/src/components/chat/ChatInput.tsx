import {
  useRef,
  useEffect,
  useState,
  type ChangeEvent,
  type KeyboardEvent,
} from 'react';
import { SendHorizontal } from 'lucide-react';
import { cn } from '@/lib/cn';

interface ChatInputProps {
  onSend: (message: string) => void;
  disabled: boolean;
  placeholder?: string;
  subtext?: string;
}

const MAX_ROWS = 4;
const LINE_HEIGHT = 24; // px, matches leading-6 / py-2.5 context

export function ChatInput({
  onSend,
  disabled,
  placeholder = '메시지를 입력하세요...',
  subtext,
}: ChatInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-focus on mount
  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  // Auto-grow textarea up to MAX_ROWS lines
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    const maxHeight = LINE_HEIGHT * MAX_ROWS + 20; // +20 for vertical padding (py-2.5 = 10px × 2)
    el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`;
  }, [value]);

  const canSend = value.trim().length > 0 && !disabled;

  function handleSend() {
    if (!canSend) return;
    onSend(value.trim());
    setValue('');
    // Reset height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  }

  function handleChange(e: ChangeEvent<HTMLTextAreaElement>) {
    setValue(e.target.value);
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  return (
    <div className="bg-white border-t border-gray-200 px-4 py-3">
      <div className="flex items-end gap-2">
      <textarea
        ref={textareaRef}
        rows={1}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        placeholder={placeholder}
        className={cn(
          'flex-1 bg-gray-50 rounded-2xl px-4 py-2.5',
          'text-sm text-gray-900 placeholder:text-gray-400',
          'resize-none leading-6 outline-none',
          'transition-colors duration-150',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          'focus:bg-white focus:ring-1 focus:ring-brand/30',
          'border border-transparent focus:border-brand/20',
        )}
        style={{ overflowY: 'auto' }}
      />
      <button
        type="button"
        onClick={handleSend}
        disabled={!canSend}
        aria-label="메시지 보내기"
        className={cn(
          'shrink-0 h-10 w-10 rounded-full',
          'flex items-center justify-center',
          'bg-brand text-white',
          'transition-all duration-150',
          'disabled:opacity-40 disabled:cursor-not-allowed',
          'enabled:hover:brightness-110 enabled:active:scale-95',
        )}
      >
        <SendHorizontal className="h-4 w-4" />
      </button>
      </div>
      {subtext && (
        <p className="text-[10px] text-[#555d6d] text-center mt-1.5">{subtext}</p>
      )}
    </div>
  );
}
