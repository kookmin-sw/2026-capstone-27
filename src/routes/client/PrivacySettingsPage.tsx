import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Info, Bell, Brain, Share2 } from 'lucide-react';
import { cn } from '@/lib/cn';
import { Button, Card } from '@/components/ui';
import { Header } from '@/components/layout/Header';

// ─── Toggle component ────────────────────────────────────────────────────────

interface ToggleRowProps {
  icon: React.ReactNode;
  label: string;
  description: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}

function ToggleRow({ icon, label, description, checked, onChange }: ToggleRowProps) {
  return (
    <div className="flex items-start gap-3 py-3">
      <div className="flex-shrink-0 w-9 h-9 rounded-full bg-blue-50 flex items-center justify-center mt-0.5">
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-gray-900">{label}</p>
        <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">{description}</p>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={cn(
          'relative flex-shrink-0 inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 focus-visible:ring-offset-2',
          checked ? 'bg-brand' : 'bg-gray-300',
        )}
      >
        <span
          className={cn(
            'inline-block h-4 w-4 transform rounded-full bg-white shadow-sm transition-transform duration-200',
            checked ? 'translate-x-6' : 'translate-x-1',
          )}
        />
      </button>
    </div>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function PrivacySettingsPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [shareCase, setShareCase] = useState(true);
  const [aiImprovement, setAiImprovement] = useState(false);
  const [pushNotifications, setPushNotifications] = useState(true);

  function handleSave() {
    // Navigate to the final review page after saving settings
    navigate(`/briefs/${id}/review`);
  }

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="개인정보 설정"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 px-4 py-6 space-y-4 pb-28">
        {/* Intro section */}
        <div className="space-y-1.5">
          <h2 className="text-base font-bold text-gray-900">
            개인정보 공유 설정
          </h2>
          <p className="text-sm text-gray-500 leading-relaxed">
            변호사와 상담 진행을 위해 필요한 정보 공유 범위를 설정해 주세요.
            설정은 언제든지 변경할 수 있습니다.
          </p>
        </div>

        {/* Data sharing toggles */}
        <Card padding="md">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
            데이터 공유
          </p>

          <ToggleRow
            icon={<Share2 size={17} className="text-brand" />}
            label="사건 상세 공유"
            description="매칭된 변호사에게 사건의 세부 내용을 공유합니다. 상담 품질 향상을 위해 권장됩니다."
            checked={shareCase}
            onChange={setShareCase}
          />

          <div className="h-px bg-gray-100" />

          <ToggleRow
            icon={<Brain size={17} className="text-purple-500" />}
            label="AI 모델 개선 동의"
            description="AI 분석 품질 향상을 위해 익명화된 사건 데이터를 활용합니다. 개인 식별 정보는 포함되지 않습니다."
            checked={aiImprovement}
            onChange={setAiImprovement}
          />

          <div className="h-px bg-gray-100" />

          <ToggleRow
            icon={<Bell size={17} className="text-amber-500" />}
            label="푸시 알림 허용"
            description="변호사 응답, 상담 일정 등 중요한 알림을 푸시 메시지로 받습니다."
            checked={pushNotifications}
            onChange={setPushNotifications}
          />
        </Card>

        {/* Current selections summary */}
        <Card padding="md">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">
            현재 설정 요약
          </p>
          <ul className="space-y-2">
            <li className="flex items-center justify-between text-sm">
              <span className="text-gray-700">사건 상세 공유</span>
              <span className={cn('font-medium', shareCase ? 'text-brand' : 'text-gray-400')}>
                {shareCase ? '허용' : '거부'}
              </span>
            </li>
            <li className="flex items-center justify-between text-sm">
              <span className="text-gray-700">AI 모델 개선</span>
              <span className={cn('font-medium', aiImprovement ? 'text-brand' : 'text-gray-400')}>
                {aiImprovement ? '동의' : '미동의'}
              </span>
            </li>
            <li className="flex items-center justify-between text-sm">
              <span className="text-gray-700">푸시 알림</span>
              <span className={cn('font-medium', pushNotifications ? 'text-brand' : 'text-gray-400')}>
                {pushNotifications ? '허용' : '거부'}
              </span>
            </li>
          </ul>
        </Card>

        {/* Privacy policy info box */}
        <div className="rounded-xl bg-blue-50 p-4 flex gap-3">
          <Info size={16} className="text-brand flex-shrink-0 mt-0.5" aria-hidden="true" />
          <div className="space-y-1.5">
            <p className="text-xs font-semibold text-blue-800">개인정보 보호 정책</p>
            <p className="text-xs text-blue-700 leading-relaxed">
              SHIELD는 수집된 모든 개인정보를 암호화하여 안전하게 보관합니다.
              공유된 정보는 법률 상담 목적으로만 사용되며, 제3자에게 무단 제공되지 않습니다.
            </p>
            <p className="text-xs text-blue-600 leading-relaxed">
              자세한 내용은{' '}
              <span className="underline font-medium">개인정보 처리방침</span>을
              확인해 주세요.
            </p>
          </div>
        </div>

        {/* Legal consent notice */}
        <p className="text-xs text-gray-400 text-center leading-relaxed">
          설정 저장 시 SHIELD의 개인정보 처리방침 및 데이터 공유 조건에
          동의하는 것으로 간주합니다.
        </p>
      </main>

      {/* Fixed bottom CTA */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 px-4 py-4 safe-area-bottom">
        <Button variant="primary" fullWidth size="lg" onClick={handleSave}>
          설정 저장
        </Button>
      </div>
    </div>
  );
}
