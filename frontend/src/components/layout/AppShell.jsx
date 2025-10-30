import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import useUIStore from '../../store/ui';
import { cn } from '../../lib/utils';

const AppShell = () => {
  const { sidebarCollapsed } = useUIStore();

  return (
    <div className="flex h-screen bg-surface-bg">
      {/* Sidebar */}
      <Sidebar />
      
      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Header */}
        <Header />
        
        {/* Content area */}
        <main className="flex-1 overflow-auto p-6">
          <div className={cn(
            'mx-auto transition-all duration-300',
            sidebarCollapsed ? 'max-w-7xl' : 'max-w-6xl'
          )}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default AppShell;
