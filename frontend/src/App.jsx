import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import useUIStore from './store/ui';
import useAuthStore from './store/auth';
import { ROLE } from './constants/roles';

// Layout components
import AppShell from './components/layout/AppShell';

// Landing page
import Landing from './features/landing/Landing';

// Auth pages
import Login from './features/auth/Login';
import Register from './features/auth/Register';
import ForgotPassword from './features/auth/ForgotPassword';
import ResetPassword from './features/auth/ResetPassword';

// Feature pages
import Dashboard from './features/dashboard/Dashboard';
import ScansList from './features/scans/ScansList';
import NewScan from './features/scans/NewScan';
import ScanDetail from './features/scans/ScanDetail';
import Reports from './features/reports/Reports';
import GeminiReportDetail from './features/reports/GeminiReportDetail';
import Notifications from './features/notifications/Notifications';
import Settings from './features/settings/Settings';
import APIKeys from './features/apikeys/APIKeys';
import UserManagement from './features/users/UserManagement';
import Forbidden from './features/errors/Forbidden';

// Mock server setup
// MSW is initialized in main.jsx during development

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Protected route component
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();

  console.log('ProtectedRoute check:', { isAuthenticated, user });

  if (!isAuthenticated) {
    console.log('Not authenticated, redirecting to login');
    return <Navigate to="/auth/login" replace />;
  }

  console.log('Authenticated, rendering children');
  return children;
};

const RoleProtectedRoute = ({ allowedRoles, children }) => {
  const { user } = useAuthStore();
  const role = user?.role ?? ROLE.VIEWER;

  if (!allowedRoles.includes(role)) {
    return <Navigate to="/403" replace />;
  }

  return children;
};

// Auth route component (redirect if already authenticated)
const AuthRoute = ({ children }) => {
  const { isAuthenticated } = useAuthStore();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
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
            {/* Landing page - root */}
            <Route path="/" element={<Landing />} />
            
            {/* Auth routes - always accessible */}
            <Route
              path="/auth/login"
              element={<Login />}
            />
            <Route
              path="/auth/register"
              element={<Register />}
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

            <Route path="/403" element={<Forbidden />} />

            {/* Protected routes - Dashboard and app routes */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route index element={<Dashboard />} />

              {/* Scans routes */}
              <Route
                path="scans"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN, ROLE.ANALYST]}>
                    <ScansList />
                  </RoleProtectedRoute>
                }
              />
              <Route
                path="scans/history"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN, ROLE.ANALYST]}>
                    <ScansList />
                  </RoleProtectedRoute>
                }
              />
              <Route
                path="scans/new"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN, ROLE.ANALYST]}>
                    <NewScan />
                  </RoleProtectedRoute>
                }
              />
              <Route
                path="scans/:scanId"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN, ROLE.ANALYST]}>
                    <ScanDetail />
                  </RoleProtectedRoute>
                }
              />

              {/* Reports routes */}
              <Route path="reports" element={<Reports />} />
              <Route path="reports/:scanId" element={<GeminiReportDetail />} />

              {/* Notifications routes */}
              <Route path="notifications" element={<Notifications />} />

              {/* Settings routes */}
              <Route
                path="settings"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN, ROLE.ANALYST, ROLE.VIEWER]}>
                    <Settings />
                  </RoleProtectedRoute>
                }
              />

              {/* API Keys routes */}
              <Route
                path="apikeys"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN]}>
                    <APIKeys />
                  </RoleProtectedRoute>
                }
              />

              {/* User Management routes */}
              <Route
                path="users"
                element={
                  <RoleProtectedRoute allowedRoles={[ROLE.ADMIN]}>
                    <UserManagement />
                  </RoleProtectedRoute>
                }
              />

              {/* Add more routes here as we build them */}
              {/* Catch-all route removed to allow proper navigation */}
            </Route>
          </Routes>
        </div>
      </Router>

      {/* React Query DevTools - only in development */}
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}

export default App;
