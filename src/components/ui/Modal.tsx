import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/cn';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children?: ReactNode;
  className?: string;
  /**
   * Prevent closing the modal by clicking the backdrop.
   * Defaults to false.
   */
  disableBackdropClose?: boolean;
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  className,
  disableBackdropClose = false,
}: ModalProps) {
  const overlayRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Lock body scroll while open
  useEffect(() => {
    if (isOpen) {
      const prev = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = prev;
      };
    }
  }, [isOpen]);

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!disableBackdropClose && e.target === overlayRef.current) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div
      ref={overlayRef}
      onClick={handleBackdropClick}
      className={cn(
        // Full-screen overlay
        'fixed inset-0 z-50 flex items-center justify-center',
        'bg-black/50 backdrop-blur-sm',
        // Fade-in animation
        'animate-in fade-in duration-200',
      )}
      aria-modal="true"
      role="dialog"
      aria-labelledby={title ? 'modal-title' : undefined}
    >
      <div
        ref={contentRef}
        className={cn(
          // Card shell
          'relative w-full max-w-md mx-4 bg-white rounded-card shadow-xl',
          // Slide + fade in
          'animate-in fade-in zoom-in-95 duration-200',
          className,
        )}
      >
        {/* Header */}
        {title && (
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-gray-100">
            <h2
              id="modal-title"
              className="text-base font-semibold text-[#1E293B] leading-none"
            >
              {title}
            </h2>
            <button
              onClick={onClose}
              className={cn(
                'flex items-center justify-center w-7 h-7 rounded-lg',
                'text-[#64748B] hover:text-[#1E293B] hover:bg-gray-100',
                'transition-colors duration-150 cursor-pointer',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
              )}
              aria-label="Close modal"
            >
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        )}

        {/* Close button when no title */}
        {!title && (
          <button
            onClick={onClose}
            className={cn(
              'absolute top-4 right-4 flex items-center justify-center w-7 h-7 rounded-lg',
              'text-[#64748B] hover:text-[#1E293B] hover:bg-gray-100',
              'transition-colors duration-150 cursor-pointer',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
            )}
            aria-label="Close modal"
          >
            <X size={16} aria-hidden="true" />
          </button>
        )}

        {/* Body */}
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}
