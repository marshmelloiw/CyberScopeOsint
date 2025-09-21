import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '../../lib/utils';
import useUIStore from '../../store/ui';
import {
  Home,
  Search,
  Users,
  BarChart3,
  Bell,
  Key,
  UserCog,
  Settings,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

const navigation = [
  { name: 'Dashboard', href: '/', icon: Home },
  {
    name: 'Scans',
    href: '/scans',
    icon: Search,
    children: [
      { name: 'New Scan', href: '/scans/new' },
      { name: 'History', href: '/scans/history' },
    ],
  },
  {
    name: 'Entities',
    href: '/entities',
    icon: Users,
    children: [
      { name: 'Email/Breach', href: '/entities/email' },
      { name: 'Domain & IP', href: '/entities/domain' },
      { name: 'Ports & Services', href: '/entities/ports' },
      { name: 'Social Monitor', href: '/entities/social' },
      { name: 'Links & Files', href: '/entities/links' },
    ],
  },
  { name: 'Reports', href: '/reports', icon: BarChart3 },
  { name: 'Notifications', href: '/notifications', icon: Bell },
  { name: 'API Keys', href: '/apikeys', icon: Key },
  { name: 'User Management', href: '/users', icon: UserCog },
  { name: 'Settings', href: '/settings', icon: Settings },
];

const Sidebar = () => {
  const { sidebarCollapsed, toggleSidebar } = useUIStore();
  const location = useLocation();

  const isActive = (href) => {
    if (href === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(href);
  };

  return (
    <div className={cn(
      'flex h-full flex-col bg-surface-panel/80 backdrop-blur-sm border-r border-surface-border transition-all duration-300',
      sidebarCollapsed ? 'w-16' : 'w-64'
    )}>
      {/* Header */}
      <div className="flex h-16 items-center justify-between px-4 border-b border-surface-border">
        {!sidebarCollapsed && (
          <div className="flex items-center space-x-2">
            <img src="/src/assets/logo.svg" alt="CyberScope" className="h-8 w-8" />
            <span className="text-lg font-bold text-white">CyberScope</span>
          </div>
        )}
        <button
          onClick={toggleSidebar}
          className="p-1 rounded-lg hover:bg-surface-border transition-colors"
        >
          {sidebarCollapsed ? (
            <ChevronRight className="h-5 w-5 text-white" />
          ) : (
            <ChevronLeft className="h-5 w-5 text-white" />
          )}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-2 py-4">
        {navigation.map((item) => {
          const isItemActive = isActive(item.href);
          
          return (
            <div key={item.name}>
              <Link
                to={item.href}
                className={cn(
                  'group flex items-center px-2 py-2 text-sm font-medium rounded-lg transition-colors',
                  isItemActive
                    ? 'bg-primary-600 text-white'
                    : 'text-surface-muted hover:bg-surface-border hover:text-white'
                )}
              >
                <item.icon className={cn(
                  'mr-3 h-5 w-5 flex-shrink-0',
                  sidebarCollapsed && 'mr-0'
                )} />
                {!sidebarCollapsed && item.name}
              </Link>
              
              {/* Submenu */}
              {item.children && !sidebarCollapsed && isItemActive && (
                <div className="ml-6 mt-1 space-y-1">
                  {item.children.map((child) => (
                    <Link
                      key={child.name}
                      to={child.href}
                      className={cn(
                        'group flex items-center px-2 py-2 text-sm rounded-lg transition-colors',
                        location.pathname === child.href
                          ? 'text-primary-400'
                          : 'text-surface-muted hover:text-white'
                      )}
                    >
                      {child.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="border-t border-surface-border p-4">
        {!sidebarCollapsed && (
          <div className="text-xs text-surface-muted text-center">
            CyberScope OSINT v1.0.0
          </div>
        )}
      </div>
    </div>
  );
};

export default Sidebar;
