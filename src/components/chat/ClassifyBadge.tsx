import { Edit2 } from 'lucide-react';
import { cn } from '@/lib/cn';
import { Badge } from '@/components/ui';

const DOMAIN_LABELS: Record<string, string> = {
  CIVIL: '민사',
  CRIMINAL: '형사',
  LABOR: '노동',
  SCHOOL_VIOLENCE: '학교폭력',
};

function resolveLabel(value: string): string {
  return DOMAIN_LABELS[value] ?? value;
}

interface ClassifyBadgeProps {
  primaryField: string[];
  tags: string[];
  onEdit?: () => void;
}

export function ClassifyBadge({ primaryField, tags, onEdit }: ClassifyBadgeProps) {
  return (
    <div
      className={cn(
        'bg-blue-50 border border-blue-200 rounded-card p-3',
        'flex flex-col gap-2',
      )}
    >
      {/* Header row */}
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-blue-700 tracking-wide uppercase">
          분류 결과
        </span>
        {onEdit && (
          <button
            type="button"
            onClick={onEdit}
            aria-label="분류 결과 수정"
            className={cn(
              'h-6 w-6 rounded-md flex items-center justify-center',
              'text-blue-500 hover:text-blue-700 hover:bg-blue-100',
              'transition-colors duration-150',
            )}
          >
            <Edit2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>

      {/* Primary fields */}
      {primaryField.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {primaryField.map((field) => (
            <Badge key={field} variant="primary" size="sm">
              {resolveLabel(field)}
            </Badge>
          ))}
        </div>
      )}

      {/* Tags */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {tags.map((tag) => (
            <Badge key={tag} variant="default" size="sm">
              {resolveLabel(tag)}
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
