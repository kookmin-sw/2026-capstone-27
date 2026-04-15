import { useQuery } from '@tanstack/react-query';
import { lawyerApi } from '@/lib/lawyerApi';

const KEYS = {
  all: ['lawyers'] as const,
  list: () => [...KEYS.all, 'list'] as const,
  detail: (id: string) => [...KEYS.all, 'detail', id] as const,
};

/** 변호사 목록 (의뢰인용) */
export function useLawyerList(
  page = 0,
  size = 20,
  specialization?: string,
) {
  return useQuery({
    queryKey: [...KEYS.list(), page, size, specialization],
    queryFn: async () => {
      const { data } = await lawyerApi.getList(page, size, specialization);
      return data.data;
    },
  });
}

/** 변호사 상세 */
export function useLawyerDetail(id: string) {
  return useQuery({
    queryKey: KEYS.detail(id),
    queryFn: async () => {
      const { data } = await lawyerApi.getById(id);
      return data.data;
    },
    enabled: !!id,
  });
}
