import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { consultationApi } from '@/lib/consultationApi';
import type {
  CreateConsultationRequest,
  MessageResponse,
} from '@/types/consultation';

const KEYS = {
  all: ['consultations'] as const,
  list: () => [...KEYS.all, 'list'] as const,
  detail: (id: string) => [...KEYS.all, 'detail', id] as const,
};

/** 상담 목록 */
export function useConsultationList(page = 0, size = 20) {
  return useQuery({
    queryKey: [...KEYS.list(), page, size],
    queryFn: async () => {
      const { data } = await consultationApi.getList(page, size);
      return data.data;
    },
  });
}

/** 상담 상세 */
export function useConsultationDetail(id: string) {
  return useQuery({
    queryKey: KEYS.detail(id),
    queryFn: async () => {
      const { data } = await consultationApi.getById(id);
      return data.data;
    },
    enabled: !!id,
  });
}

/** 새 상담 생성
 *
 *  BE는 상담 생성 시 welcomeMessage를 chat_messages에 자동 저장하고
 *  Response에도 함께 내려준다. ChatPage 진입 시 getMessages 로딩 지연없이
 *  첫 AI 메시지가 보이도록 messages 쿼리 캐시를 미리 채워놓는다. */
export function useCreateConsultation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateConsultationRequest) =>
      consultationApi.create(request),
    onSuccess: (response) => {
      const created = response.data.data;
      queryClient.invalidateQueries({ queryKey: KEYS.list() });

      // welcomeMessage를 messages 쿼리 캐시에 미리 주입.
      // useChat 내부 queryFn이 content 배열만 반환하므로 MessageResponse[]로 맞춰서 저장.
      if (created?.welcomeMessage) {
        const welcomeMsg: MessageResponse = {
          messageId: `welcome-${created.consultationId}`,
          role: 'CHATBOT',
          content: created.welcomeMessage,
          createdAt: created.createdAt,
        };
        queryClient.setQueryData(
          ['messages', created.consultationId],
          [welcomeMsg],
        );
      }
    },
  });
}

/** 분류 수정 — 3단계 분류 체계({domains, subDomains, tags}) 로 전달.
 *
 *  NewConsultationPage의 CategoryPicker + toClassificationRequest() 와 동일한
 *  입력 형식을 기대한다. 나중 AnalyzingPage에 "분류 수정" UI를 붙일 때
 *  CategoryPicker를 그대로 재사용해 이 mutation 인자로 넣으면 된다. */
export function useUpdateClassify(consultationId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: {
      domains: string[];
      subDomains: string[];
      tags: string[];
    }) => consultationApi.updateClassify(consultationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: KEYS.detail(consultationId),
      });
      // 상담 상세 정보의 ai*/user* 필드가 바뀌므로 목록도 함께 스타일 처리.
      queryClient.invalidateQueries({ queryKey: KEYS.list() });
    },
  });
}

/** 의뢰서 생성 요청 */
export function useRequestAnalyze(consultationId: string) {
  return useMutation({
    mutationFn: () => consultationApi.requestAnalyze(consultationId),
  });
}
