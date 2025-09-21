import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '../../lib/utils';
import useUIStore from '../../store/ui';
import useAuthStore from '../../store/auth';
import {
  Search,
  Bell,
  Sun,
  Moon,
  User,
} from 'lucide-react';

const Header = () => {
  const { theme, toggleTheme, globalSearchOpen, setGlobalSearchOpen } = useUIStore();
  const { user, logout } = useAuthStore();
  const [showUserMenu, setShowUserMenu] = useState(false);

  const handleLogout = () => {
    logout();
    setShowUserMenu(false);
  };

  return (
    <header className="flex h-16 items-center justify-between border-b border-surface-border bg-surface-panel/80 backdrop-blur-sm px-6">
      {/* Left section */}
      <div className="flex items-center space-x-4">
        <button
          onClick={() => setGlobalSearchOpen(true)}
          className="flex items-center space-x-2 rounded-lg bg-surface-panel px-3 py-2 text-sm text-surface-muted hover:bg-surface-border hover:text-white transition-colors"
        >
          <Search className="h-4 w-4" />
          <span>Search...</span>
          <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border border-surface-border bg-surface-panel px-1.5 font-mono text-xs text-surface-muted">
            <span className="text-xs">⌘</span>K
          </kbd>
        </button>
      </div>

      {/* Right section */}
      <div className="flex items-center space-x-4">
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
          to="/notifications"
          className="relative rounded-lg p-2 text-surface-muted hover:bg-surface-border hover:text-white transition-colors"
        >
          <Bell className="h-5 w-5" />
          <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-xs text-white">
            3
          </span>
        </Link>

        {/* User menu */}
        <div className="relative">
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
            <div className="absolute right-0 top-full mt-2 w-48 rounded-lg bg-surface-panel border border-surface-border shadow-lg">
              <div className="p-2">
                <Link
                  to="/profile"
                  className="block w-full rounded-lg px-3 py-2 text-sm text-white hover:bg-surface-border transition-colors"
                  onClick={() => setShowUserMenu(false)}
                >
                  Profile
                </Link>
                <Link
                  to="/settings"
                  className="block w-full rounded-lg px-3 py-2 text-sm text-white hover:bg-surface-border transition-colors"
                  onClick={() => setShowUserMenu(false)}
                >
                  Settings
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

      {/* Global search modal */}
      {globalSearchOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-20">
          <div className="fixed inset-0 bg-black/50" onClick={() => setGlobalSearchOpen(false)} />
          <div className="relative w-full max-w-2xl">
            <div className="glass rounded-2xl border border-white/10 p-4">
              <div className="flex items-center space-x-2">
                <MagnifyingGlassIcon className="h-5 w-5 text-surface-muted" />
                <input
                  type="text"
                  placeholder="Search entities, scans, reports..."
                  className="flex-1 bg-transparent text-white placeholder:text-surface-muted focus:outline-none"
                  autoFocus
                />
                <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border border-surface-border bg-surface-panel px-1.5 font-mono text-xs text-surface-muted">
                  ESC
                </kbd>
              </div>
              
              {/* Search results would go here */}
              <div className="mt-4 text-sm text-surface-muted">
                Start typing to search...
              </div>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};

export default Header;
