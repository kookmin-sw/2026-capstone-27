import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';

const KEYS = {
  stats: ['admin', 'stats'] as const,
  alerts: ['admin', 'alerts'] as const,
  pending: ['admin', 'pending'] as const,
  lawyer: (id: string) => ['admin', 'lawyer', id] as const,
  checks: (id: string) => ['admin', 'checks', id] as const,
  docs: (id: string) => ['admin', 'docs', id] as const,
  logs: ['admin', 'logs'] as const,
};

export function useAdminStats() {
  return useQuery({
    queryKey: KEYS.stats,
    queryFn: async () => {
      const { data } = await adminApi.getStats();
      return data.data;
    },
  });
}

export function useAdminAlerts() {
  return useQuery({
    queryKey: KEYS.alerts,
    queryFn: async () => {
      const { data } = await adminApi.getAlerts();
      return data.data;
    },
  });
}

export function usePendingLawyers(page = 0, size = 20, keyword?: string, status?: string) {
  return useQuery({
    queryKey: [...KEYS.pending, page, size, keyword, status],
    queryFn: async () => {
      const { data } = await adminApi.getPendingLawyers(page, size, keyword, status);
      return data.data;
    },
  });
}

export function useAdminLawyerDetail(id: string) {
  return useQuery({
    queryKey: KEYS.lawyer(id),
    queryFn: async () => {
      const { data } = await adminApi.getLawyerDetail(id);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useVerificationChecks(id: string) {
  return useQuery({
    queryKey: KEYS.checks(id),
    queryFn: async () => {
      const { data } = await adminApi.getVerificationChecks(id);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useLawyerDocuments(id: string) {
  return useQuery({
    queryKey: KEYS.docs(id),
    queryFn: async () => {
      const { data } = await adminApi.getDocuments(id);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useProcessVerification(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { status: string; reason?: string }) =>
      adminApi.processVerification(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.pending });
      queryClient.invalidateQueries({ queryKey: KEYS.lawyer(id) });
      queryClient.invalidateQueries({ queryKey: KEYS.logs });
    },
  });
}

/** 처리 이력 조회 */
export function useVerificationLogs(
  page = 0,
  size = 20,
  filters?: { status?: string; startDate?: string; endDate?: string },
) {
  return useQuery({
    queryKey: [...KEYS.logs, page, size, filters],
    queryFn: async () => {
      const { data } = await adminApi.getVerificationLogs(page, size, filters);
      return data.data;
    },
  });
}
