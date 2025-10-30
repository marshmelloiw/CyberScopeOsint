import React, { useEffect, PropsWithChildren } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import useUIStore from './store/ui';
import useAuthStore from './store/auth';

import AppShell from './components/layout/AppShell';

import Login from './features/auth/Login';
import Register from './features/auth/Register';
import ForgotPassword from './features/auth/ForgotPassword';
import ResetPassword from './features/auth/ResetPassword';

import Dashboard from './features/dashboard/Dashboard';
import ScansList from './features/scans/ScansList';
import NewScan from './features/scans/NewScan';
import EmailBreach from './features/entities/EmailBreach';
import DomainIP from './features/entities/DomainIP';
import Reports from './features/reports/Reports';
import Notifications from './features/notifications/Notifications';
import Settings from './features/settings/Settings';
import APIKeys from './features/apikeys/APIKeys';
import UserManagement from './features/users/UserManagement';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

const ProtectedRoute = ({ children }: PropsWithChildren) => {
  const { isAuthenticated } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/auth/login" replace />;
  return <>{children}</>;
};

const AuthRoute = ({ children }: PropsWithChildren) => {
  const { isAuthenticated } = useAuthStore();
  if (isAuthenticated) return <Navigate to="/" replace />;
  return <>{children}</>;
};

function App() {
  const { initializeTheme } = useUIStore();

  useEffect(() => {
    initializeTheme();
  }, [initializeTheme]);

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="App">
          <Routes>
            <Route
              path="/auth/login"
              element={
                <AuthRoute>
                  <Login />
                </AuthRoute>
              }
            />
            <Route
              path="/auth/register"
              element={
                <AuthRoute>
                  <Register />
                </AuthRoute>
              }
            />
            <Route
              path="/auth/forgot-password"
              element={
                <AuthRoute>
                  <ForgotPassword />
                </AuthRoute>
              }
            />
            <Route
              path="/auth/reset-password"
              element={
                <AuthRoute>
                  <ResetPassword />
                </AuthRoute>
              }
            />

            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route index element={<Dashboard />} />
              <Route path="scans" element={<ScansList />} />
              <Route path="scans/new" element={<NewScan />} />
              <Route path="entities" element={<Navigate to="/entities/email" replace />} />
              <Route path="entities/email" element={<EmailBreach />} />
              <Route path="entities/domain" element={<DomainIP />} />
              <Route path="entities/ports" element={<div className="p-6"><h1 className="text-2xl font-bold text-white mb-4">Ports & Services</h1><p className="text-surface-muted">Coming soon...</p></div>} />
              <Route path="entities/social" element={<div className="p-6"><h1 className="text-2xl font-bold text-white mb-4">Social Monitor</h1><p className="text-surface-muted">Coming soon...</p></div>} />
              <Route path="entities/links" element={<div className="p-6"><h1 className="text-2xl font-bold text-white mb-4">Links & Files</h1><p className="text-surface-muted">Coming soon...</p></div>} />
              <Route path="reports" element={<Reports />} />
              <Route path="notifications" element={<Notifications />} />
              <Route path="settings" element={<Settings />} />
              <Route path="apikeys" element={<APIKeys />} />
              <Route path="users" element={<UserManagement />} />
            </Route>
          </Routes>
        </div>
      </Router>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}

export default App;


