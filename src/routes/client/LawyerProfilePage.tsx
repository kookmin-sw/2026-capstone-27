import { useNavigate, useParams } from 'react-router-dom';
import { User, Shield, MapPin, Award, Briefcase } from 'lucide-react';
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

                {/* Specialization */}
                {lawyer.specializations && (
                  <div className="flex flex-wrap justify-center gap-1.5">
                    <Badge variant="primary" size="sm">
                      {DOMAIN_LABELS[lawyer.specializations] ?? lawyer.specializations}
                    </Badge>
                  </div>
                )}

                {/* Experience + Region */}
                <div className="flex items-center gap-3 text-sm text-gray-500">
                  <span>{lawyer.experienceYears}년 경력</span>
                  {lawyer.region && (
                    <>
                      <span className="text-gray-300">|</span>
                      <span className="flex items-center gap-1">
                        <MapPin size={13} aria-hidden="true" />
                        {lawyer.region}
                      </span>
                    </>
                  )}
                </div>

                {/* Case count */}
                {lawyer.caseCount > 0 && (
                  <div className="flex items-center gap-1.5 text-sm text-gray-500">
                    <Briefcase size={13} aria-hidden="true" />
                    <span>처리 사건 {lawyer.caseCount}건</span>
                  </div>
                )}
              </div>
            </Card>

            {/* ── Bio card ── */}
            {lawyer.bio && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">소개</h3>
                <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {lawyer.bio}
                </p>
              </Card>
            )}

            {/* ── Tags card ── */}
            {lawyer.tags && lawyer.tags.length > 0 && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">태그</h3>
                <div className="flex flex-wrap gap-1.5">
                  {lawyer.tags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-flex items-center px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-medium"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </Card>
            )}

            {/* ── Certifications card ── */}
            {lawyer.certifications && lawyer.certifications.length > 0 && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">자격/인증</h3>
                <ul className="space-y-1.5">
                  {lawyer.certifications.map((cert) => (
                    <li key={cert} className="flex items-center gap-2 text-sm text-gray-700">
                      <Award size={14} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
                      {cert}
                    </li>
                  ))}
                </ul>
              </Card>
            )}
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
