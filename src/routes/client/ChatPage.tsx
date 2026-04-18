import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useChat } from '@/hooks/useChat';
import { useConsultationDetail, useRequestAnalyze } from '@/hooks/useConsultation';
import { Button, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { ChatBubble } from '@/components/chat/ChatBubble';
import { ChatInput } from '@/components/chat/ChatInput';
import { TypingIndicator } from '@/components/chat/TypingIndicator';
import { ClassifyBadge } from '@/components/chat/ClassifyBadge';
import { DOMAIN_LABELS, CONSULTATION_STATUS_LABELS } from '@/lib/constants';

// ─── page ────────────────────────────────────────────────────────────────────

export function ChatPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // Consultation detail for status/domain info
  const { data: consultation } = useConsultationDetail(id);

  // Chat state
  const {
    messages,
    isLoading,
    isSending,
    allCompleted,
    classification,
    scrollRef,
    sendMessage,
  } = useChat(id);

  // Analyze mutation
  const { mutate: requestAnalyze, isPending: isAnalyzing } = useRequestAnalyze(id);

  // ── status-based redirects ──────────────────────────────────────────────
  useEffect(() => {
    if (!consultation) return;
    if (consultation.status === 'ANALYZING') {
      navigate(`/consultations/${id}/analyzing`, { replace: true });
      return;
    }
    if (
      consultation.status === 'AWAITING_CONFIRM' ||
      consultation.status === 'CONFIRMED' ||
      consultation.status === 'REJECTED'
    ) {
      navigate('/briefs', { replace: true });
    }
  }, [consultation, id, navigate]);

  // ── derive page title ───────────────────────────────────────────────────
  function buildTitle(): string {
    if (!consultation) return '상담';
    const statusLabel = CONSULTATION_STATUS_LABELS[consultation.status];
    const domainLabel =
      consultation.primaryField && consultation.primaryField.length > 0
        ? consultation.primaryField.map((f) => DOMAIN_LABELS[f] ?? f).join(' · ')
        : null;
    return domainLabel ? `${domainLabel} 상담` : statusLabel ?? '상담';
  }

  // ── handle "의뢰서 생성" click ──────────────────────────────────────────
  function handleRequestAnalyze() {
    requestAnalyze(undefined, {
      onSuccess: () => {
        navigate(`/consultations/${id}/analyzing`);
      },
    });
  }

  // ── loading ─────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header
          title="상담"
          showBack
          onBack={() => navigate('/consultations')}
        />
        <div className="flex-1 flex items-center justify-center">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* ── header ─────────────────────────────────────────────────────── */}
      <Header
        title={buildTitle()}
        showBack
        onBack={() => navigate('/consultations')}
      />

      {/* ── AI notice bar ─────────────────────────────────────────────── */}
      <div className="bg-gray-50/50 border-b border-[#e0e2e6] flex items-center gap-2 px-4 py-2">
        <span className="w-1.5 h-1.5 rounded-full bg-brand/50" />
        <p className="text-[11px] font-medium text-[#555d6d]">
          AI는 법률 상담이 아닌 정보 정리를 도와드립니다
        </p>
      </div>

      {/* ── scrollable message area ─────────────────────────────────────── */}
      <div
        ref={scrollRef}
        className={cn(
          'chat-viewport flex-1 py-3',
          'scrollbar-hide',
        )}
      >
        {/* Messages */}
        {messages.map((msg, idx) => (
          <ChatBubble
            key={idx}
            sender={msg.role}
            content={msg.content}
            timestamp={msg.createdAt}
          />
        ))}

        {/* Typing indicator */}
        {isSending && <TypingIndicator />}

        {/* Classification badge — shown after last message when present */}
        {classification && !isSending && (
          <div className="px-4 pt-2 pb-1">
            <ClassifyBadge
              primaryField={classification.primaryField}
              tags={classification.tags}
            />
          </div>
        )}
      </div>

      {/* ── bottom area ─────────────────────────────────────────────────── */}
      <div className="bg-white safe-area-bottom">
        {/* "의뢰서 생성" CTA — shown when allCompleted */}
        {allCompleted && (
          <div className="px-4 pt-3 pb-1">
            <Button
              variant="primary"
              size="lg"
              fullWidth
              isLoading={isAnalyzing}
              leftIcon={<FileText size={18} />}
              onClick={handleRequestAnalyze}
              className="bg-brand shadow-md"
            >
              의뢰서 생성
            </Button>
          </div>
        )}

        {/* Chat input */}
        <ChatInput
          onSend={sendMessage}
          disabled={isSending || allCompleted}
          placeholder={allCompleted ? '상담이 완료되었습니다' : '메시지를 입력하세요...'}
        />
      </div>
    </div>
  );
}
