import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { inboxApi } from '@/lib/inboxApi';

const KEYS = {
  all: ['inbox'] as const,
  list: () => [...KEYS.all, 'list'] as const,
  stats: () => [...KEYS.all, 'stats'] as const,
  detail: (id: string) => [...KEYS.all, 'detail', id] as const,
};

export function useInboxList(page = 0, size = 20, status?: string) {
  return useQuery({
    queryKey: [...KEYS.list(), page, size, status],
    queryFn: async () => {
      const { data } = await inboxApi.getList(page, size, status);
      return data.data;
    },
  });
}

export function useInboxStats() {
  return useQuery({
    queryKey: KEYS.stats(),
    queryFn: async () => {
      const { data } = await inboxApi.getStats();
      return data.data;
    },
  });
}

export function useInboxDetail(id: string) {
  return useQuery({
    queryKey: KEYS.detail(id),
    queryFn: async () => {
      const { data } = await inboxApi.getById(id);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useUpdateInboxStatus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (status: 'CONFIRMED' | 'REJECTED') =>
      inboxApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.list() });
      queryClient.invalidateQueries({ queryKey: KEYS.stats() });
      queryClient.invalidateQueries({ queryKey: KEYS.detail(id) });
    },
  });
}
