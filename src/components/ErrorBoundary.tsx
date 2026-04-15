import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <div className="min-h-dvh flex flex-col items-center justify-center gap-5 px-6 text-center">
          <div className="inline-flex rounded-full bg-red-50 p-4">
            <AlertTriangle className="h-8 w-8 text-red-500" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">
              문제가 발생했습니다
            </h1>
            <p className="mt-2 text-sm text-gray-500 max-w-sm">
              예상치 못한 오류가 발생했습니다. 페이지를 새로고침하거나 잠시 후
              다시 시도해 주세요.
            </p>
            {import.meta.env.DEV && this.state.error && (
              <pre className="mt-4 max-w-md overflow-auto rounded-lg bg-gray-100 p-3 text-left text-xs text-red-600">
                {this.state.error.message}
              </pre>
            )}
          </div>
          <div className="flex gap-3">
            <button
              onClick={this.handleReset}
              className="inline-flex items-center gap-2 rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-white hover:bg-blue-600 transition-colors"
            >
              <RefreshCw className="h-4 w-4" />
              다시 시도
            </button>
            <a
              href="/"
              className="inline-flex items-center rounded-full border border-gray-300 px-5 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
            >
              홈으로 이동
            </a>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
