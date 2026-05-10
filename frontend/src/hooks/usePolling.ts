import { useEffect, useRef, useState } from 'react';

interface UsePollingOptions<T> {
  /** 폴링할 함수 */
  fn: () => Promise<T>;
  /** 폴링 간격 (ms) */
  interval?: number;
  /** 최대 폴링 시간 (ms) */
  maxDuration?: number;
  /** 폴링 활성화 여부 */
  enabled?: boolean;
  /** 폴링 중단 조건 */
  shouldStop?: (data: T) => boolean;
  /** 중단 시 콜백 */
  onComplete?: (data: T) => void;
  /** 타임아웃 시 콜백 */
  onTimeout?: () => void;
}

export function usePolling<T>({
  fn,
  interval = 5000,
  maxDuration = 60000,
  enabled = true,
  shouldStop,
  onComplete,
  onTimeout,
}: UsePollingOptions<T>) {
  const [data, setData] = useState<T | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval>>(undefined);
  const startRef = useRef<number>(0);

  useEffect(() => {
    if (!enabled) return;

    setIsPolling(true);
    startRef.current = Date.now();

    const poll = async () => {
      try {
        const result = await fn();
        setData(result);

        if (shouldStop?.(result)) {
          clearInterval(timerRef.current);
          setIsPolling(false);
          onComplete?.(result);
          return;
        }

        if (Date.now() - startRef.current >= maxDuration) {
          clearInterval(timerRef.current);
          setIsPolling(false);
          onTimeout?.();
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '폴링 실패');
      }
    };

    // 즉시 1회 실행
    poll();
    timerRef.current = setInterval(poll, interval);

    return () => {
      clearInterval(timerRef.current);
      setIsPolling(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]);

  return { data, isPolling, error };
}
