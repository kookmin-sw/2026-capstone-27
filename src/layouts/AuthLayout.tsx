import { Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <div className="min-h-dvh flex flex-col bg-white">
      {/* Content fills the full viewport — each auth page handles its own hero/gradient */}
      <div className="flex-1 flex flex-col items-center justify-start">
        <div className="w-full sm:max-w-[448px] sm:mx-auto">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
