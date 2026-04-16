import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { briefApi } from '@/lib/briefApi';
import type { BriefUpdateRequest } from '@/types/brief';

const KEYS = {
  all: ['briefs'] as const,
  list: () => [...KEYS.all, 'list'] as const,
  detail: (id: string) => [...KEYS.all, 'detail', id] as const,
  recommendations: (id: string) => [...KEYS.all, 'recommendations', id] as const,
  deliveries: (id: string) => [...KEYS.all, 'deliveries', id] as const,
};

/** 의뢰서 목록 */
export function useBriefList(page = 0, size = 20) {
  return useQuery({
    queryKey: [...KEYS.list(), page, size],
    queryFn: async () => {
      const { data } = await briefApi.getList(page, size);
      return data.data;
    },
  });
}

/** 의뢰서 상세 */
export function useBriefDetail(id: string) {
  return useQuery({
    queryKey: KEYS.detail(id),
    queryFn: async () => {
      const { data } = await briefApi.getById(id);
      return data.data;
    },
    enabled: !!id,
  });
}

/** 의뢰서 수정 */
export function useUpdateBrief(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (updates: BriefUpdateRequest) => briefApi.update(id, updates),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.detail(id) });
    },
  });
}

/** 의뢰서 확정 */
export function useConfirmBrief(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => briefApi.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: KEYS.list() });
    },
  });
}

/** 변호사 추천 */
export function useLawyerRecommendations(briefId: string, enabled = true) {
  return useQuery({
    queryKey: KEYS.recommendations(briefId),
    queryFn: async () => {
      const { data } = await briefApi.getRecommendations(briefId);
      return data.data;
    },
    enabled: !!briefId && enabled,
  });
}

/** 의뢰서 전달 */
export function useDeliverBrief(briefId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (lawyerId: string) => briefApi.deliver(briefId, lawyerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.deliveries(briefId) });
    },
  });
}

/** 전달 현황 */
export function useDeliveries(briefId: string) {
  return useQuery({
    queryKey: KEYS.deliveries(briefId),
    queryFn: async () => {
      const { data } = await briefApi.getDeliveries(briefId);
      return data.data.deliveries;
    },
    enabled: !!briefId,
  });
}
