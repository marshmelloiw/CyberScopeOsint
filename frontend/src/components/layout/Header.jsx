import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '../../lib/utils';
import useUIStore from '../../store/ui';
import useAuthStore from '../../store/auth';
import api, { endpoints } from '../../lib/axios';
import {
  Bell,
  Sun,
  Moon,
  User,
} from 'lucide-react';

const Header = () => {
  const { theme, toggleTheme } = useUIStore();
  const { user, logout } = useAuthStore();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const handleLogout = () => {
    logout();
    setShowUserMenu(false);
  };

  // Fetch unread notification count
  const fetchUnreadCount = async () => {
    try {
      const userId = user?.id;
      if (!userId) {
        setUnreadCount(0);
        return;
      }

      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId;
      const response = await api.get(endpoints.notifications.list, {
        params: { userId: numericUserId }
      });

      if (response.data && response.data.unreadCount !== undefined) {
        setUnreadCount(response.data.unreadCount);
      }
    } catch (error) {
      console.error('Error fetching unread count:', error);
      setUnreadCount(0);
    }
  };

  useEffect(() => {
    if (user?.id) {
      fetchUnreadCount();
      // Refresh every 30 seconds
      const interval = setInterval(fetchUnreadCount, 30000);
      
      // Listen for notification updates
      const handleNotificationUpdate = () => {
        fetchUnreadCount();
      };
      
      window.addEventListener('notificationUpdated', handleNotificationUpdate);
      
      return () => {
        clearInterval(interval);
        window.removeEventListener('notificationUpdated', handleNotificationUpdate);
      };
    }
  }, [user?.id]);

  return (
    <header className="flex h-16 items-center justify-between border-b border-surface-border bg-surface-panel/80 backdrop-blur-sm px-6 relative z-50">
      {/* Left section - empty for now */}
      <div></div>
      
      {/* Right section */}
      <div className="flex items-center space-x-4 ml-auto">
        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          className="rounded-lg p-2 text-surface-muted hover:bg-surface-border hover:text-white transition-colors"
        >
          {theme === 'dark' ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}
        </button>

        {/* Notifications */}
        <Link
          to="/dashboard/notifications"
          className="relative rounded-lg p-2 text-surface-muted hover:bg-surface-border hover:text-white transition-colors"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 flex min-w-[1rem] h-4 items-center justify-center rounded-full bg-danger text-xs text-white px-1">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </Link>

        {/* User menu */}
        <div className="relative z-50">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="flex items-center space-x-2 rounded-lg p-2 text-surface-muted hover:bg-surface-border hover:text-white transition-colors"
          >
            {user?.avatar ? (
              <img
                src={user.avatar}
                alt={user.name}
                className="h-8 w-8 rounded-full"
              />
            ) : (
              <User className="h-8 w-8" />
            )}
            <span className="text-sm font-medium text-white">{user?.name || 'User'}</span>
          </button>

          {showUserMenu && (
            <div className="absolute right-0 top-full mt-2 w-48 rounded-lg bg-surface-panel border border-surface-border shadow-lg z-50">
              <div className="p-2">
                <Link
                  to="/dashboard/settings"
                  className="block w-full rounded-lg px-3 py-2 text-sm text-white hover:bg-surface-border transition-colors"
                  onClick={() => setShowUserMenu(false)}
                >
                  Profile
                </Link>
                <hr className="my-2 border-surface-border" />
                <button
                  onClick={handleLogout}
                  className="block w-full rounded-lg px-3 py-2 text-sm text-left text-danger hover:bg-surface-border transition-colors"
                >
                  Logout
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
