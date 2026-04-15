import type { InputHTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftAddon?: ReactNode;
  rightAddon?: ReactNode;
  containerClassName?: string;
}

export function Input({
  label,
  error,
  helperText,
  leftAddon,
  rightAddon,
  id,
  className,
  containerClassName,
  disabled,
  ...rest
}: InputProps) {
  const inputId = id ?? (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
  const hasError = Boolean(error);

  return (
    <div className={cn('flex flex-col gap-1.5', containerClassName)}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-[#1E293B] leading-none"
        >
          {label}
        </label>
      )}

      <div className="relative flex items-center">
        {leftAddon && (
          <span className="absolute left-3 flex items-center text-[#64748B] pointer-events-none">
            {leftAddon}
          </span>
        )}

        <input
          id={inputId}
          disabled={disabled}
          className={cn(
            // Base
            'w-full rounded-xl border bg-white px-3 py-2.5 text-sm text-[#1E293B]',
            'placeholder:text-[#64748B]',
            'transition-colors duration-150',
            // Focus ring
            'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
            // Normal border
            !hasError && 'border-gray-300',
            // Error border
            hasError && 'border-red-500 focus:ring-red-400/30 focus:border-red-500',
            // Left/right addon padding
            leftAddon && 'pl-9',
            rightAddon && 'pr-9',
            // Disabled
            disabled && 'opacity-50 cursor-not-allowed bg-gray-50',
            className,
          )}
          aria-invalid={hasError}
          aria-describedby={
            hasError
              ? `${inputId}-error`
              : helperText
              ? `${inputId}-helper`
              : undefined
          }
          {...rest}
        />

        {rightAddon && (
          <span className="absolute right-3 flex items-center text-[#64748B] pointer-events-none">
            {rightAddon}
          </span>
        )}
      </div>

      {hasError && (
        <p
          id={`${inputId}-error`}
          className="text-xs text-red-500 leading-snug"
          role="alert"
        >
          {error}
        </p>
      )}

      {!hasError && helperText && (
        <p
          id={`${inputId}-helper`}
          className="text-xs text-[#64748B] leading-snug"
        >
          {helperText}
        </p>
      )}
    </div>
  );
}
