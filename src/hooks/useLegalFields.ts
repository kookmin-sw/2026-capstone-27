import { useQuery } from '@tanstack/react-query';
import { consultationApi } from '@/lib/consultationApi';

/** Maps BE enum values to Korean labels */
const FIELD_LABELS: Record<string, string> = {
  CIVIL: '민사',
  CRIMINAL: '형사',
  LABOR: '노동',
  SCHOOL_VIOLENCE: '학교폭력',
};

export function useLegalFields() {
  return useQuery({
    queryKey: ['legal-fields'],
    queryFn: async () => {
      const { data } = await consultationApi.getLegalFields();
      return data.data.map((field) => ({
        value: field,
        label: FIELD_LABELS[field] ?? field,
      }));
    },
    staleTime: 1000 * 60 * 60, // 1 hour cache — these rarely change
  });
}
