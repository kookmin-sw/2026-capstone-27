import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  User,
  Mail,
  Phone,
  MapPin,
  FileText,
  CheckCircle,
  XCircle,
  AlertTriangle,
  ExternalLink,
} from 'lucide-react';
import {
  useAdminLawyerDetail,
  useVerificationChecks,
  useLawyerDocuments,
  useProcessVerification,
} from '@/hooks/useAdmin';
import { Card, Badge, Button, Spinner, Modal, Input } from '@/components/ui';
import type { VerificationCheck } from '@/lib/adminApi';

type ActionType = 'APPROVED' | 'REJECTED' | 'SUPPLEMENT_REQUESTED';

const resultIcon: Record<string, React.ReactNode> = {
  PASS: <CheckCircle className="h-4 w-4 text-green-500" />,
  FAIL: <XCircle className="h-4 w-4 text-red-500" />,
  WARN: <AlertTriangle className="h-4 w-4 text-yellow-500" />,
};

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
          <div className="flex items-center gap-2 text-sm">
            <Mail className="h-4 w-4 text-gray-400" />
            <span className="text-gray-700">{lawyer.email}</span>
          </div>
          {lawyer.phone && (
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-gray-400" />
              <span className="text-gray-700">{lawyer.phone}</span>
            </div>
          )}
          {lawyer.officeAddress && (
            <div className="flex items-center gap-2 text-sm">
              <MapPin className="h-4 w-4 text-gray-400" />
              <span className="text-gray-700">{lawyer.officeAddress}</span>
            </div>
          )}
          <div className="flex items-center gap-2 text-sm">
            <FileText className="h-4 w-4 text-gray-400" />
            <span className="text-gray-700">자격번호: {lawyer.licenseNumber}</span>
          </div>
        </div>
        <div className="flex flex-wrap gap-1.5 mt-3">
          {lawyer.specializations.map((s) => (
            <Badge key={s} variant="default" size="sm">{s}</Badge>
          ))}
        </div>
      </Card>

      {/* 자동 검증 결과 */}
      <Card padding="md">
        <h2 className="font-bold text-gray-900 mb-3">자동 검증 결과</h2>
        {checksLoading ? (
          <Spinner size="sm" />
        ) : !checks?.length ? (
          <p className="text-sm text-gray-400">검증 데이터가 없습니다</p>
        ) : (
          <div className="space-y-2">
            {checks.map((c: VerificationCheck, idx: number) => (
              <div
                key={idx}
                className="flex items-start gap-3 rounded-lg border border-gray-100 p-3"
              >
                {resultIcon[c.result] ?? resultIcon.WARN}
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900">{c.checkType}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{c.detail}</p>
                </div>
              </div>
            ))}
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
            {docs.map((d: { id: string; fileName: string; fileUrl: string; uploadedAt: string }) => (
              <li key={d.id}>
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
                      {new Date(d.uploadedAt).toLocaleDateString('ko-KR')}
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
