import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { consultationApi } from '@/lib/consultationApi';
import type { DomainType } from '@/types/enums';

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

/** 새 상담 생성 */
export function useCreateConsultation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (domain: DomainType | null) => consultationApi.create(domain),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.list() });
    },
  });
}

/** 분류 수정 */
export function useUpdateClassify(consultationId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { primaryField: string[]; tags: string[] }) =>
      consultationApi.updateClassify(consultationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: KEYS.detail(consultationId),
      });
    },
  });
}

/** 의뢰서 생성 요청 */
export function useRequestAnalyze(consultationId: string) {
  return useMutation({
    mutationFn: () => consultationApi.requestAnalyze(consultationId),
  });
}
