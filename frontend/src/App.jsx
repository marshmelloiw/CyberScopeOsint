import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import useUIStore from './store/ui';
import useAuthStore from './store/auth';

// Layout components
import AppShell from './components/layout/AppShell';

// Auth pages
import Login from './features/auth/Login';
import Register from './features/auth/Register';
import ForgotPassword from './features/auth/ForgotPassword';
import ResetPassword from './features/auth/ResetPassword';

// Feature pages
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

// Auth route component (redirect if already authenticated)
const AuthRoute = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
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
            {/* Auth routes */}
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
            
            {/* Protected routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route index element={<Dashboard />} />
              
              {/* Scans routes */}
              <Route path="scans" element={<ScansList />} />
              <Route path="scans/new" element={<NewScan />} />
              
              {/* Entities routes */}
              <Route path="entities/email" element={<EmailBreach />} />
              <Route path="entities/domain" element={<DomainIP />} />
              
              {/* Reports routes */}
              <Route path="reports" element={<Reports />} />
              
              {/* Notifications routes */}
              <Route path="notifications" element={<Notifications />} />
              
              {/* Settings routes */}
              <Route path="settings" element={<Settings />} />
              
              {/* API Keys routes */}
              <Route path="apikeys" element={<APIKeys />} />
              
              {/* User Management routes */}
              <Route path="users" element={<UserManagement />} />
              
              {/* Add more routes here as we build them */}
              <Route path="*" element={<Navigate to="/" replace />} />
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
