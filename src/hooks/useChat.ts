import { useCallback, useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { consultationApi } from '@/lib/consultationApi';
import { useChatStore } from '@/stores/chatStore';
import type { MessageResponse } from '@/types/consultation';

const KEYS = {
  messages: (id: string) => ['messages', id] as const,
};

export function useChat(consultationId: string) {
  const queryClient = useQueryClient();
  const {
    messages,
    isSending,
    allCompleted,
    classification,
    setMessages,
    addMessage,
    setIsSending,
    setAllCompleted,
    setClassification,
    reset,
  } = useChatStore();

  const scrollRef = useRef<HTMLDivElement>(null);

  // 메시지 목록 초기 로딩
  const { isLoading } = useQuery({
    queryKey: KEYS.messages(consultationId),
    queryFn: async () => {
      const { data } = await consultationApi.getMessages(consultationId);
      setMessages(data.data);
      return data.data;
    },
    enabled: !!consultationId,
  });

  // 스크롤 하단 고정
  const scrollToBottom = useCallback(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // 메시지 전송 (낙관적 UI)
  const sendMessage = useCallback(
    async (content: string) => {
      if (!content.trim() || isSending) return;

      // 1. 낙관적 UI — 사용자 메시지 즉시 표시
      const optimisticMsg: MessageResponse = {
        sender: 'USER',
        content,
        timestamp: new Date().toISOString(),
      };
      addMessage(optimisticMsg);
      setIsSending(true);

      try {
        // 2. API 호출
        const { data } = await consultationApi.sendMessage(
          consultationId,
          content,
        );
        const res = data.data;

        // 3. AI 응답 추가
        addMessage({
          sender: 'CHATBOT',
          content: res.content,
          timestamp: res.timestamp,
        });

        // 4. 분류 업데이트
        if (res.classification) {
          setClassification(res.classification);
        }

        // 5. 완료 여부
        if (res.allCompleted) {
          setAllCompleted(true);
        }

        // 캐시 무효화
        queryClient.invalidateQueries({
          queryKey: KEYS.messages(consultationId),
        });
      } catch (error) {
        // PII 에러 등 핸들링 — 에러 메시지를 시스템 메시지로 표시
        const errorMsg =
          error instanceof Error ? error.message : '메시지 전송에 실패했습니다';

        addMessage({
          sender: 'SYSTEM',
          content: errorMsg,
          timestamp: new Date().toISOString(),
        });
      } finally {
        setIsSending(false);
      }
    },
    [
      consultationId,
      isSending,
      addMessage,
      setIsSending,
      setClassification,
      setAllCompleted,
      queryClient,
    ],
  );

  // 언마운트 시 리셋
  useEffect(() => {
    return () => reset();
  }, [reset]);

  return {
    messages,
    isLoading,
    isSending,
    allCompleted,
    classification,
    scrollRef,
    sendMessage,
  };
}
