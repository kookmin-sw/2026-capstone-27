import { useNavigate, useParams } from 'react-router-dom';
import { User, Shield, Mail, Phone, MapPin } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useLawyerDetail } from '@/hooks/useLawyer';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';

// ─── page ────────────────────────────────────────────────────────────────────

export function LawyerProfilePage() {
  const navigate = useNavigate();
  const { id = '' } = useParams<{ id: string }>();

  const { data: lawyer, isLoading } = useLawyerDetail(id);

  const isVerified = lawyer?.verificationStatus === 'VERIFIED';

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="변호사 프로필"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 px-4 py-4 pb-28 space-y-3">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-64">
            <Spinner size="lg" />
          </div>
        )}

        {/* Not found */}
        {!isLoading && !lawyer && (
          <div className="flex flex-col items-center justify-center gap-4 pt-20 text-center">
            <div className="w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center">
              <User size={36} className="text-gray-300" aria-hidden="true" />
            </div>
            <p className="text-base font-medium text-gray-500">
              변호사 정보를 찾을 수 없습니다
            </p>
          </div>
        )}

        {lawyer && (
          <>
            {/* ── Profile card ── */}
            <Card padding="md">
              <div className="flex flex-col items-center text-center gap-3">
                {/* Avatar */}
                <div
                  className={cn(
                    'w-20 h-20 rounded-full bg-gray-100',
                    'flex items-center justify-center overflow-hidden',
                  )}
                >
                  {lawyer.profileImageUrl ? (
                    <img
                      src={lawyer.profileImageUrl}
                      alt={lawyer.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <User size={36} className="text-gray-400" aria-hidden="true" />
                  )}
                </div>

                {/* Name */}
                <h2 className="text-xl font-bold text-gray-900">{lawyer.name}</h2>

                {/* Verification badge */}
                {isVerified ? (
                  <div className="inline-flex items-center gap-1.5 text-sm font-medium text-green-600 bg-green-50 px-3 py-1 rounded-full">
                    <Shield size={14} aria-hidden="true" />
                    인증된 변호사
                  </div>
                ) : (
                  <div className="inline-flex items-center text-sm font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
                    심사 중
                  </div>
                )}

                {/* Specializations */}
                {lawyer.specializations.length > 0 && (
                  <div className="flex flex-wrap justify-center gap-1.5">
                    {lawyer.specializations.map((spec) => (
                      <Badge key={spec} variant="primary" size="sm">
                        {DOMAIN_LABELS[spec] ?? spec}
                      </Badge>
                    ))}
                  </div>
                )}

                {/* Experience */}
                <p className="text-sm text-gray-500">{lawyer.experienceYears}년 경력</p>
              </div>
            </Card>

            {/* ── Introduction card ── */}
            {lawyer.introduction && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">소개</h3>
                <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {lawyer.introduction}
                </p>
              </Card>
            )}

            {/* ── Contact info card ── */}
            <Card padding="md">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">연락처</h3>
              <ul className="space-y-3">
                {/* Email — always present */}
                <li className="flex items-center gap-3 text-sm text-gray-700">
                  <Mail size={16} className="flex-shrink-0 text-gray-400" aria-hidden="true" />
                  <span className="break-all">{lawyer.email}</span>
                </li>

                {/* Phone — optional */}
                {lawyer.phone && (
                  <li className="flex items-center gap-3 text-sm text-gray-700">
                    <Phone size={16} className="flex-shrink-0 text-gray-400" aria-hidden="true" />
                    <span>{lawyer.phone}</span>
                  </li>
                )}

                {/* Office address — optional */}
                {lawyer.officeAddress && (
                  <li className="flex items-start gap-3 text-sm text-gray-700">
                    <MapPin
                      size={16}
                      className="flex-shrink-0 text-gray-400 mt-0.5"
                      aria-hidden="true"
                    />
                    <span>{lawyer.officeAddress}</span>
                  </li>
                )}
              </ul>
            </Card>
          </>
        )}
      </main>

      {/* ── Bottom fixed CTA ── */}
      {lawyer && (
        <div
          className={cn(
            'fixed bottom-0 left-0 right-0 z-30',
            'bg-white border-t border-gray-200 px-4 py-3 safe-area-bottom',
          )}
        >
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => navigate('/briefs')}
          >
            의뢰서 전달하기
          </Button>
        </div>
      )}
    </div>
  );
}
