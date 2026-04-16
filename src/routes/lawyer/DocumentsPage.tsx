import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText, Upload, AlertCircle } from 'lucide-react';
import { cn } from '@/lib/cn';
import api from '@/lib/api';
import { Button, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';

// ─── types ───────────────────────────────────────────────────────────────────

interface DocumentItem {
  documentId: string;
  fileName: string;
  fileUrl?: string;
  createdAt: string;
}

// ─── helpers ─────────────────────────────────────────────────────────────────

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
const ACCEPTED_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];
const ACCEPTED_EXTS = '.pdf,.jpg,.jpeg,.png';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

function fileTypeLabel(name: string): string {
  const ext = name.split('.').pop()?.toUpperCase() ?? '';
  return ext;
}

// ─── page ────────────────────────────────────────────────────────────────────

export function DocumentsPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [uploadSuccess, setUploadSuccess] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  useEffect(() => {
    fetchDocuments();
  }, []);

  async function fetchDocuments() {
    setIsLoading(true);
    try {
      const { data } = await api.get<{ data: DocumentItem[] }>('/lawyers/me/documents');
      setDocuments(data.data ?? []);
    } finally {
      setIsLoading(false);
    }
  }

  function validateFile(file: File): string | null {
    if (!ACCEPTED_TYPES.includes(file.type)) {
      return 'PDF, JPG, PNG 파일만 업로드 가능합니다.';
    }
    if (file.size > MAX_FILE_SIZE) {
      return '파일 크기는 10MB 이하여야 합니다.';
    }
    return null;
  }

  function handleFileSelect(file: File) {
    setUploadError('');
    setUploadSuccess('');
    const error = validateFile(file);
    if (error) {
      setUploadError(error);
      setSelectedFile(null);
      return;
    }
    setSelectedFile(file);
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
    // Reset input so same file can be re-selected
    e.target.value = '';
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragging(true);
  }

  function handleDragLeave() {
    setIsDragging(false);
  }

  async function handleUpload() {
    if (!selectedFile) return;
    setIsUploading(true);
    setUploadError('');
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      await api.post('/lawyers/me/documents', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setUploadSuccess(`"${selectedFile.name}" 업로드가 완료되었습니다.`);
      setSelectedFile(null);
      await fetchDocuments();
    } catch {
      setUploadError('업로드에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="서류 관리" showBack onBack={() => navigate(-1)} />

      <main className="flex-1 px-4 py-4 pb-10 space-y-4">
        {/* Upload section */}
        <Card padding="md">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">서류 업로드</h2>

          {/* Drag & drop area */}
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
            className={cn(
              'flex flex-col items-center justify-center gap-2',
              'border-2 border-dashed rounded-xl py-8 px-4',
              'cursor-pointer transition-colors duration-150',
              isDragging
                ? 'border-brand bg-blue-50'
                : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50',
            )}
          >
            <Upload
              size={28}
              className={cn(
                'transition-colors',
                isDragging ? 'text-brand' : 'text-gray-400',
              )}
            />
            <p className="text-sm text-gray-600 text-center">
              파일을 드래그하거나 클릭하여 업로드
            </p>
            <p className="text-xs text-gray-400">PDF, JPG, PNG · 최대 10MB</p>

            {selectedFile && (
              <div className="mt-2 flex items-center gap-2 px-3 py-2 bg-blue-50 rounded-lg">
                <FileText size={14} className="text-blue-500 flex-shrink-0" />
                <span className="text-xs text-blue-700 font-medium truncate max-w-[200px]">
                  {selectedFile.name}
                </span>
              </div>
            )}
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPTED_EXTS}
            onChange={handleInputChange}
            className="hidden"
            aria-label="파일 선택"
          />

          {/* Error / success messages */}
          {uploadError && (
            <div className="mt-3 flex items-center gap-2 px-3 py-2 bg-red-50 rounded-lg">
              <AlertCircle size={14} className="text-red-500 flex-shrink-0" />
              <p className="text-xs text-red-600">{uploadError}</p>
            </div>
          )}
          {uploadSuccess && (
            <div className="mt-3 px-3 py-2 bg-green-50 rounded-lg">
              <p className="text-xs text-green-700 font-medium">{uploadSuccess}</p>
            </div>
          )}

          {/* Upload button */}
          {selectedFile && !uploadError && (
            <div className="mt-3">
              <Button
                variant="primary"
                fullWidth
                isLoading={isUploading}
                onClick={handleUpload}
                leftIcon={<Upload size={15} />}
              >
                업로드
              </Button>
            </div>
          )}
        </Card>

        {/* Documents list */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">제출된 서류</h2>

          {isLoading ? (
            <div className="flex items-center justify-center h-32">
              <Spinner size="md" />
            </div>
          ) : documents.length === 0 ? (
            <Card padding="md" className="text-center py-8">
              <FileText size={32} className="text-gray-300 mx-auto mb-2" />
              <p className="text-sm text-gray-400">제출된 서류가 없습니다</p>
            </Card>
          ) : (
            <div className="flex flex-col gap-2">
              {documents.map((doc) => (
                <Card key={doc.documentId} padding="sm">
                  <div className="flex items-center gap-3">
                    <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-gray-100 flex-shrink-0">
                      <FileText size={18} className="text-gray-500" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {doc.fileName}
                      </p>
                      <p className="text-xs text-gray-400 mt-0.5">
                        {fileTypeLabel(doc.fileName)} · {formatDate(doc.createdAt)}
                      </p>
                    </div>
                    {doc.fileUrl && (
                      <a
                        href={doc.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs text-brand font-medium hover:text-blue-700 transition-colors flex-shrink-0"
                      >
                        보기
                      </a>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
