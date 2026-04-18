import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import {
  useAdminLawyerDetail,
  useVerificationChecks,
  useLawyerDocuments,
  useProcessVerification,
} from '@/hooks/useAdmin';
import { Spinner, Modal, Input, Button } from '@/components/ui';
import type { VerificationChecks as VerificationChecksType } from '@/lib/adminApi';

type ActionType = 'APPROVED' | 'REJECTED' | 'SUPPLEMENT_REQUESTED';

const STATUS_BADGE: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: 'bg-[#f1f0e8]', text: 'text-[#5f5e5a]', label: '승인 대기' },
  REVIEWING: { bg: 'bg-[#e8f0fc]', text: 'text-[#0c5fa5]', label: '검토 중' },
  SUPPLEMENT_REQUESTED: { bg: 'bg-[#faeeda]', text: 'text-[#854f0b]', label: '보완 요청' },
  VERIFIED: { bg: 'bg-[#eaf3de]', text: 'text-[#3b6e11]', label: '승인 완료' },
  REJECTED: { bg: 'bg-[#fcebeb]', text: 'text-[#a32c2c]', label: '거절' },
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const CHECKLIST_ITEMS: { key: keyof Omit<VerificationChecksType, 'lawyerId' | 'completedCount' | 'totalCount' | 'updatedAt'>; label: string }[] = [
  { key: 'licenseVerified', label: '변호사 자격 증빙 확인' },
  { key: 'documentMatched', label: '서류 정보 일치' },
  { key: 'specializationValid', label: '전문 분야 기재 적절' },
  { key: 'experienceVerified', label: '경력 정보 확인' },
  { key: 'duplicateSignup', label: '중복 가입 여부 확인' },
  { key: 'documentComplete', label: '필수 서류 누락 없음' },
];

const VERIFY_ITEMS: { key: keyof Omit<VerificationChecksType, 'lawyerId' | 'completedCount' | 'totalCount' | 'updatedAt'>; label: string }[] = [
  { key: 'emailDuplicate', label: '이메일 중복' },
  { key: 'phoneDuplicate', label: '전화번호 중복' },
  { key: 'nameDuplicate', label: '동일 이름 계정' },
  { key: 'requiredFields', label: '필수 항목 누락' },
];

export function LawyerReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: lawyer, isLoading: lawyerLoading } = useAdminLawyerDetail(id!);
  const { data: checks, isLoading: checksLoading } = useVerificationChecks(id!);
  const { data: docs, isLoading: docsLoading } = useLawyerDocuments(id!);
  const { mutate: process, isPending: processing } = useProcessVerification(id!);

  const [modalAction, setModalAction] = useState<ActionType | null>(null);
  const [reason, setReason] = useState('');

  const handleSubmit = () => {
    if (!modalAction) return;
    process(
      { status: modalAction, reason: reason || undefined },
      {
        onSuccess: () => {
          setModalAction(null);
          setReason('');
          navigate('/admin/lawyers');
        },
      },
    );
  };

  if (lawyerLoading) return <Spinner size="lg" text="변호사 정보 로딩 중..." />;
  if (!lawyer) {
    return (
      <div className="text-center py-20">
        <p className="text-[#6b7280] text-[13px]">변호사 정보를 찾을 수 없습니다</p>
        <button
          type="button"
          onClick={() => navigate('/admin/lawyers')}
          className="mt-4 text-[12px] text-[#1a6de0]"
        >
          목록으로
        </button>
      </div>
    );
  }

  const badge = STATUS_BADGE[lawyer.verificationStatus] ?? STATUS_BADGE.PENDING;
  const initial = lawyer.name.charAt(0);

  // 체크리스트 진행률 계산
  const completedCount = checks?.completedCount ?? 0;
  const totalCount = checks?.totalCount ?? CHECKLIST_ITEMS.length;
  const progressPct = totalCount > 0 ? (completedCount / totalCount) * 100 : 0;

  const actionLabels: Record<ActionType, { label: string; desc: string }> = {
    APPROVED: { label: '승인', desc: '이 변호사의 인증을 승인하시겠습니까?' },
    REJECTED: { label: '거절', desc: '거절 사유를 입력해 주세요.' },
    SUPPLEMENT_REQUESTED: { label: '보완 요청', desc: '보완이 필요한 내용을 입력해 주세요.' },
  };

  return (
    <div className="space-y-4">
      {/* 헤더 영역: 뒤로가기 + 타이틀 + 상태 */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/lawyers')}
          className="w-7 h-7 rounded-full bg-[#f7f8fa] flex items-center justify-center text-[#6b7280]"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <h1 className="text-[15px] font-medium text-[#1a1a1a] flex-1">가입 신청 상세</h1>
        <span className={`${badge.bg} ${badge.text} text-[11px] font-medium h-[22px] px-3 rounded-[11px] flex items-center`}>
          {badge.label}
        </span>
      </div>

      {/* 프로필 카드 */}
      <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-3">
        <div className="flex items-start gap-3">
          <div className="w-12 h-12 rounded-full bg-[#1a6de0] flex items-center justify-center shrink-0">
            <span className="text-[18px] font-medium text-white">{initial}</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[16px] font-medium text-[#1a1a1a]">{lawyer.name}</p>
            <p className="text-[11px] text-[#adb5b8]">{lawyer.bio || 'lawyer@shield.com'}</p>
            <p className="text-[11px] text-[#adb5b8]">{lawyer.region || '-'}</p>
          </div>
        </div>
        {lawyer.specializations && (
          <div className="flex items-center gap-1.5 mt-2.5">
            {lawyer.specializations.split(',').map((spec) => (
              <span key={spec.trim()} className="bg-[#e8f0fc] text-[#0c447c] text-[10px] h-5 px-2 rounded-[10px] flex items-center">
                {spec.trim()}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* 기본 정보 */}
      <div>
        <p className="text-[12px] font-medium text-[#1a6de0] mb-1.5">기본 정보</p>
        <div className="h-[0.5px] bg-[#e9edef] mb-2.5" />
        <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] overflow-hidden">
          {[
            { label: '경력', value: `${lawyer.experienceYears}년` },
            { label: '사건 수', value: `${lawyer.caseCount}건` },
            { label: '활동 지역', value: lawyer.region || '-' },
          ].map((row, i, arr) => (
            <div key={row.label}>
              <div className="flex items-center px-3 h-[22px]">
                <span className="text-[12px] text-[#6b7280] w-[140px]">{row.label}</span>
                <span className="text-[12px] text-[#1a1a1a]">{row.value}</span>
              </div>
              {i < arr.length - 1 && <div className="h-[0.5px] bg-[#e9edef] mx-3" />}
            </div>
          ))}
        </div>
      </div>

      {/* 제출 서류 */}
      <div>
        <p className="text-[12px] font-medium text-[#1a6de0] mb-1.5">제출 서류</p>
        <div className="h-[0.5px] bg-[#e9edef] mb-2.5" />
        {docsLoading ? (
          <Spinner size="sm" />
        ) : !docs?.length ? (
          <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-4 text-center">
            <p className="text-[12px] text-[#6b7280]">제출된 서류가 없습니다</p>
          </div>
        ) : (
          <div className="space-y-2">
            {docs.map((d: { documentId: string; fileName: string; fileSize: number; fileType: string; fileUrl: string; createdAt: string }) => (
              <div key={d.documentId} className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-3">
                <div className="flex items-start gap-3">
                  {/* PDF 아이콘 */}
                  <div className="w-9 h-11 bg-[#e8f0fc] rounded-[6px] flex items-center justify-center shrink-0">
                    <span className="text-[10px] font-medium text-[#0c447c]">PDF</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-[12px] font-medium text-[#1a1a1a] truncate">{d.fileName}</p>
                    <p className="text-[11px] text-[#adb5b8] mt-0.5">
                      {formatFileSize(d.fileSize)} · {new Date(d.createdAt).toLocaleDateString('ko-KR')}
                    </p>
                    <div className="flex items-center gap-2 mt-1.5">
                      <a
                        href={d.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="bg-[#e8f0fc] text-[#1a6de0] text-[10px] h-[18px] px-2 rounded-[9px] flex items-center"
                      >
                        미리보기
                      </a>
                      <a
                        href={d.fileUrl}
                        download
                        className="bg-[#e8f0fc] text-[#1a6de0] text-[10px] h-[18px] px-2 rounded-[9px] flex items-center"
                      >
                        다운로드
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 자동 검증 */}
      <div>
        <p className="text-[12px] font-medium text-[#1a6de0] mb-1.5">자동 검증</p>
        <div className="h-[0.5px] bg-[#e9edef] mb-2.5" />
        {checksLoading ? (
          <Spinner size="sm" />
        ) : !checks ? (
          <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-4 text-center">
            <p className="text-[12px] text-[#6b7280]">검증 데이터가 없습니다</p>
          </div>
        ) : (
          <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] overflow-hidden">
            {VERIFY_ITEMS.map((item, i) => {
              const value = checks[item.key] as boolean;
              // emailDuplicate/phoneDuplicate/nameDuplicate: false = pass, true = warning
              // requiredFields: true = pass, false = warning
              const isPass = item.key === 'requiredFields' ? value : !value;
              return (
                <div key={item.key}>
                  <div className="flex items-center justify-between px-3 h-[20px]">
                    <span className="text-[12px] text-[#6b7280]">{item.label}</span>
                    {isPass ? (
                      <span className="text-[11px] text-[#3b6e11]">✓ 이상 없음</span>
                    ) : (
                      <span className="text-[11px] text-[#854f0b]">⚠ 주의 필요</span>
                    )}
                  </div>
                  {i < VERIFY_ITEMS.length - 1 && <div className="h-[0.5px] bg-[#e9edef] mx-3" />}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 검토 체크리스트 */}
      {checks && (
        <div>
          <p className="text-[12px] font-medium text-[#1a6de0] mb-1.5">검토 체크리스트</p>
          <div className="h-[0.5px] bg-[#e9edef] mb-2.5" />
          <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] overflow-hidden p-3">
            <div className="space-y-0">
              {CHECKLIST_ITEMS.map((item, i) => {
                const checked = checks[item.key] as boolean;
                // duplicateSignup: false = checked (no duplicate), true = unchecked
                const isChecked = item.key === 'duplicateSignup' ? !checked : checked;
                return (
                  <div key={item.key}>
                    <div className="flex items-center gap-2.5 h-[22px]">
                      <div className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 ${isChecked ? 'bg-[#1a6de0]' : 'bg-[#e9edef]'}`}>
                        {isChecked && <span className="text-[12px] font-medium text-white">✓</span>}
                      </div>
                      <span className="text-[12px] text-[#1a1a1a]">{item.label}</span>
                    </div>
                    {i < CHECKLIST_ITEMS.length - 1 && <div className="h-[0.5px] bg-[#e9edef] my-0.5" />}
                  </div>
                );
              })}
            </div>

            {/* 진행률 바 */}
            <div className="mt-3">
              <div className="h-1.5 bg-[#f7f8fa] rounded-[3px] overflow-hidden">
                <div
                  className="h-full bg-[#1a6de0] rounded-[3px] transition-all"
                  style={{ width: `${progressPct}%` }}
                />
              </div>
              <p className="text-[11px] text-[#6b7280] mt-1">{completedCount} / {totalCount} 완료</p>
            </div>
          </div>
        </div>
      )}

      {/* 액션 버튼 — 세로 스택 */}
      <div className="space-y-2 pt-2">
        <button
          type="button"
          onClick={() => setModalAction('APPROVED')}
          disabled={processing}
          className="w-full h-12 bg-[#1a6de0] rounded-[24px] text-[15px] font-medium text-white disabled:opacity-50"
        >
          승인
        </button>
        <button
          type="button"
          onClick={() => setModalAction('SUPPLEMENT_REQUESTED')}
          disabled={processing}
          className="w-full h-11 bg-white border-[1.5px] border-[#faeeda] rounded-[22px] text-[14px] font-medium text-[#854f0b] disabled:opacity-50"
        >
          보완 요청
        </button>
        <button
          type="button"
          onClick={() => setModalAction('REJECTED')}
          disabled={processing}
          className="w-full h-11 bg-white border-[1.5px] border-[#fcebeb] rounded-[22px] text-[14px] font-medium text-[#a32c2c] disabled:opacity-50"
        >
          거절
        </button>
      </div>

      {/* 확인 모달 */}
      {modalAction && (
        <Modal
          isOpen={!!modalAction}
          onClose={() => { setModalAction(null); setReason(''); }}
          title={actionLabels[modalAction].label}
        >
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              {actionLabels[modalAction].desc}
            </p>
            {modalAction !== 'APPROVED' && (
              <Input
                placeholder="사유를 입력하세요"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
            <div className="flex justify-end gap-2">
              <Button
                variant="secondary"
                onClick={() => { setModalAction(null); setReason(''); }}
              >
                취소
              </Button>
              <Button
                variant={modalAction === 'REJECTED' ? 'danger' : 'primary'}
                onClick={handleSubmit}
                disabled={processing || (modalAction !== 'APPROVED' && !reason.trim())}
              >
                {processing ? '처리 중...' : '확인'}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
