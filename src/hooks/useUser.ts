import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/lib/userApi';
import type { UserInfo } from '@/types/auth';

const KEYS = {
  all: ['users'] as const,
  me: () => [...KEYS.all, 'me'] as const,
};

/** 내 정보 조회 */
export function useMyProfile() {
  return useQuery({
    queryKey: KEYS.me(),
    queryFn: async () => {
      const { data } = await userApi.getMe();
      return data.data;
    },
  });
}

/** 내 정보 수정 */
export function useUpdateMyProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (updates: Partial<Pick<UserInfo, 'name' | 'email'>>) =>
      userApi.updateMe(updates),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.me() });
    },
  });
}
