import { Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <div className="min-h-dvh flex flex-col">
      {/* Gradient top portion */}
      <div
        className="w-full bg-gradient-to-b from-blue-500 to-blue-600"
        style={{ height: '35%', minHeight: '160px', maxHeight: '280px' }}
        aria-hidden="true"
      />

      {/* Content area — overlaps the gradient */}
      <div className="flex-1 flex flex-col items-center justify-start -mt-12 pb-8 px-4">
        <div className="w-full sm:max-w-[448px]">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
