# UI 컴포넌트 카탈로그

모든 UI 컴포넌트는 `src/components/ui/`에 위치하며, `@/components/ui` barrel에서 import합니다.

## Button

다양한 변형과 크기를 지원하는 범용 버튼.

```tsx
import { Button } from '@/components/ui';

<Button variant="primary" size="lg" fullWidth isLoading={loading}>
  텍스트
</Button>
```

| Prop | 타입 | 기본값 | 설명 |
| ---- | ---- | ---- | ---- |
| variant | `'primary' \| 'secondary' \| 'danger' \| 'kakao' \| 'naver' \| 'google'` | `'primary'` | 스타일 변형 |
| size | `'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| isLoading | `boolean` | `false` | 로딩 스피너 |
| fullWidth | `boolean` | `false` | 전체 너비 |
| leftIcon | `ReactNode` | — | 좌측 아이콘 |

## Input

라벨, 에러 메시지, 아이콘 addon을 지원하는 폼 입력.

```tsx
import { Input } from '@/components/ui';

<Input
  label="이메일"
  placeholder="example@mail.com"
  error={errors.email?.message}
  leftAddon={<Mail size={16} />}
/>
```

| Prop | 타입 | 설명 |
| ---- | ---- | ---- |
| label | `string` | 상단 라벨 |
| error | `string` | 에러 메시지 (빨간색) |
| helperText | `string` | 도움말 텍스트 |
| leftAddon | `ReactNode` | 좌측 아이콘 |
| rightAddon | `ReactNode` | 우측 아이콘 |

## Badge

상태 표시용 태그.

```tsx
import { Badge } from '@/components/ui';

<Badge variant="success" size="sm">완료</Badge>
```

| Variant | 색상 |
| ---- | ---- |
| default | 회색 |
| primary | 파란 |
| success | 녹색 |
| warning | 노란 |
| danger | 빨간 |

`BadgeVariant` 타입은 `@/components/ui/Badge`에서 export됩니다.

## Card

둥근 모서리 + 그림자가 적용된 컨테이너.

```tsx
import { Card } from '@/components/ui';

<Card padding="md">콘텐츠</Card>
```

padding: `'none' | 'sm' | 'md' | 'lg'`

## Modal

접근성을 지원하는 대화상자.

```tsx
import { Modal } from '@/components/ui';

<Modal isOpen={open} onClose={() => setOpen(false)} title="확인">
  본문
</Modal>
```

- ESC 키로 닫기
- 배경 클릭으로 닫기
- 포커스 트랩
- 페이드인 애니메이션

## Spinner

로딩 인디케이터.

```tsx
import { Spinner } from '@/components/ui';

<Spinner size="lg" text="로딩 중..." />
```

size: `'sm' | 'md' | 'lg'`

## SpecializationPicker

변호사 전문분야 선택 컴포넌트. 검색 + 아코디언 트리 + 체크박스 복합 UI.

```tsx
import { SpecializationPicker } from '@/components/ui';

<SpecializationPicker
  value={selectedSpecs}
  onChange={(v) => setValue('specializations', v)}
  error={errors.specializations?.message}
/>
```

| Prop | 타입 | 설명 |
| ---- | ---- | ---- |
| value | `string[]` | 선택된 항목 이름 배열 |
| onChange | `(value: string[]) => void` | 변경 콜백 |
| error | `string` | 에러 메시지 |

데이터: `src/lib/specializations.ts` (8개 대분류 > ~30개 중분류 > ~120개 소분류)

**두 가지 모드:**

1. 브라우징 모드 — 아코디언 트리로 탐색
2. 검색 모드 — 이름 + aliases 기반 플랫 검색, breadcrumb 경로 표시
