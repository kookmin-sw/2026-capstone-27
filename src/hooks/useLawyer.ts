import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { lawyerApi } from '@/lib/lawyerApi';
import type { VerificationRequestData } from '@/types/lawyer';

const KEYS = {
  all: ['lawyers'] as const,
  list: () => [...KEYS.all, 'list'] as const,
  detail: (id: string) => [...KEYS.all, 'detail', id] as const,
  me: () => [...KEYS.all, 'me'] as const,
  verification: () => [...KEYS.all, 'verification'] as const,
  myDocuments: () => [...KEYS.all, 'my-documents'] as const,
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

/** 내 프로필 (변호사) */
export function useMyLawyerProfile() {
  return useQuery({
    queryKey: KEYS.me(),
    queryFn: async () => {
      const { data } = await lawyerApi.getMe();
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

/** 검증 상태 확인 */
export function useVerificationStatus() {
  return useQuery({
    queryKey: KEYS.verification(),
    queryFn: async () => {
      const { data } = await lawyerApi.getVerificationStatus();
      return data.data;
    },
  });
}

/** 검증 신청 */
export function useRequestVerification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: VerificationRequestData) =>
      lawyerApi.requestVerification(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.verification() });
    },
  });
}

/** 본인 서류 목록 */
export function useMyDocuments() {
  return useQuery({
    queryKey: KEYS.myDocuments(),
    queryFn: async () => {
      const { data } = await lawyerApi.getMyDocuments();
      return data.data;
    },
  });
}

/** 서류 업로드 */
export function useUploadDocument() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => lawyerApi.uploadDocument(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.myDocuments() });
    },
  });
}
