import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  User,
  MapPin,
  CheckCircle,
  XCircle,
  ExternalLink,
  Briefcase,
  Award,
} from 'lucide-react';
import {
  useAdminLawyerDetail,
  useVerificationChecks,
  useLawyerDocuments,
  useProcessVerification,
} from '@/hooks/useAdmin';
import { Card, Badge, Button, Spinner, Modal, Input } from '@/components/ui';
import type { VerificationChecks as VerificationChecksType } from '@/lib/adminApi';

type ActionType = 'APPROVED' | 'REJECTED' | 'SUPPLEMENT_REQUESTED';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

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
        <p className="text-gray-400">변호사 정보를 찾을 수 없습니다</p>
        <Button variant="secondary" className="mt-4" onClick={() => navigate('/admin/lawyers')}>
          목록으로
        </Button>
      </div>
    );
  }

  const actionLabels: Record<ActionType, { label: string; variant: 'primary' | 'danger' | 'secondary'; desc: string }> = {
    APPROVED: { label: '승인', variant: 'primary', desc: '이 변호사의 인증을 승인하시겠습니까?' },
    REJECTED: { label: '반려', variant: 'danger', desc: '반려 사유를 입력해 주세요.' },
    SUPPLEMENT_REQUESTED: { label: '보충 요청', variant: 'secondary', desc: '보충이 필요한 내용을 입력해 주세요.' },
  };

  const checkItems: { label: string; key: keyof Omit<VerificationChecksType, 'lawyerId'>; desc: string }[] = [
    { label: '이메일 중복', key: 'emailDuplicate', desc: '이메일 중복 여부' },
    { label: '전화번호 중복', key: 'phoneDuplicate', desc: '전화번호 중복 여부' },
    { label: '이름 중복', key: 'nameDuplicate', desc: '이름 중복 여부' },
    { label: '필수 항목', key: 'requiredFields', desc: '필수 입력 항목 충족 여부' },
  ];

  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <button
        onClick={() => navigate('/admin/lawyers')}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-900"
      >
        <ArrowLeft className="h-4 w-4" />
        목록으로
      </button>

      {/* 변호사 정보 */}
      <Card padding="md">
        <h2 className="font-bold text-gray-900 mb-3">변호사 정보</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex items-center gap-2 text-sm">
            <User className="h-4 w-4 text-gray-400" />
            <span className="text-gray-700">{lawyer.name}</span>
          </div>
          {lawyer.region && (
            <div className="flex items-center gap-2 text-sm">
              <MapPin className="h-4 w-4 text-gray-400" />
              <span className="text-gray-700">{lawyer.region}</span>
            </div>
          )}
          <div className="flex items-center gap-2 text-sm">
            <Briefcase className="h-4 w-4 text-gray-400" />
            <span className="text-gray-700">경력 {lawyer.experienceYears}년 · 사건 {lawyer.caseCount}건</span>
          </div>
        </div>
        {/* Specialization */}
        {lawyer.specializations && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            <Badge variant="default" size="sm">{lawyer.specializations}</Badge>
          </div>
        )}
        {/* Bio */}
        {lawyer.bio && (
          <p className="text-sm text-gray-600 mt-3 leading-relaxed">{lawyer.bio}</p>
        )}
        {/* Tags */}
        {lawyer.tags && lawyer.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            {lawyer.tags.map((tag) => (
              <span key={tag} className="inline-flex items-center px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 text-xs font-medium">
                {tag}
              </span>
            ))}
          </div>
        )}
        {/* Certifications */}
        {lawyer.certifications && lawyer.certifications.length > 0 && (
          <div className="mt-3">
            <p className="text-xs font-semibold text-gray-500 mb-1">자격/인증</p>
            <div className="flex flex-wrap gap-1.5">
              {lawyer.certifications.map((cert) => (
                <span key={cert} className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-green-50 text-green-700 text-xs font-medium">
                  <Award className="h-3 w-3" />
                  {cert}
                </span>
              ))}
            </div>
          </div>
        )}
      </Card>

      {/* 자동 검증 결과 */}
      <Card padding="md">
        <h2 className="font-bold text-gray-900 mb-3">자동 검증 결과</h2>
        {checksLoading ? (
          <Spinner size="sm" />
        ) : !checks ? (
          <p className="text-sm text-gray-400">검증 데이터가 없습니다</p>
        ) : (
          <div className="space-y-2">
            {checkItems.map((item) => {
              const value = checks[item.key];
              const isPass = item.key === 'requiredFields' ? value : !value;
              return (
                <div
                  key={item.key}
                  className="flex items-start gap-3 rounded-lg border border-gray-100 p-3"
                >
                  {isPass ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : (
                    <XCircle className="h-4 w-4 text-red-500" />
                  )}
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900">{item.label}</p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {item.key === 'requiredFields'
                        ? (value ? '모든 필수 항목 충족' : '필수 항목 미충족')
                        : (value ? '중복 발견' : '중복 없음')
                      }
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* 제출 서류 */}
      <Card padding="md">
        <h2 className="font-bold text-gray-900 mb-3">제출 서류</h2>
        {docsLoading ? (
          <Spinner size="sm" />
        ) : !docs?.length ? (
          <p className="text-sm text-gray-400">제출된 서류가 없습니다</p>
        ) : (
          <ul className="space-y-2">
            {docs.map((d: { documentId: string; fileName: string; fileSize: number; fileType: string; fileUrl: string; uploadedAt: string }) => (
              <li key={d.documentId}>
                <a
                  href={d.fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-between rounded-lg border border-gray-100 px-3 py-2 hover:bg-gray-50"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {d.fileName}
                    </p>
                    <p className="text-xs text-gray-400">
                      {d.fileType} · {formatFileSize(d.fileSize)} · {new Date(d.uploadedAt).toLocaleDateString('ko-KR')}
                    </p>
                  </div>
                  <ExternalLink className="h-4 w-4 text-gray-400 shrink-0 ml-2" />
                </a>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {/* 액션 바 */}
      <div className="flex flex-wrap gap-2">
        <Button
          variant="primary"
          onClick={() => setModalAction('APPROVED')}
          disabled={processing}
        >
          승인
        </Button>
        <Button
          variant="danger"
          onClick={() => setModalAction('REJECTED')}
          disabled={processing}
        >
          반려
        </Button>
        <Button
          variant="secondary"
          onClick={() => setModalAction('SUPPLEMENT_REQUESTED')}
          disabled={processing}
        >
          보충 요청
        </Button>
      </div>

      {/* 모달 */}
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
                variant={actionLabels[modalAction].variant}
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
