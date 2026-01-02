import React, { useState, useRef, useMemo, useCallback, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { User, Sun, Moon, Shield, Globe, Palette, Eye, EyeOff, Upload } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';
import useUIStore from '../../store/ui';
import useAuthStore from '../../store/auth';
import MFASetup from '../../components/auth/MFASetup';

const Settings = () => {
  const [activeTab, setActiveTab] = useState('profile');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [showMFASetup, setShowMFASetup] = useState(false);

  const { theme, setTheme, sidebarCollapsed, toggleSidebar } = useUIStore();
  const { user, updateProfile } = useAuthStore();
  const phoneRef = useRef(null);

  const [profileData, setProfileData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    role: user?.role || '',
    avatar: user?.avatar || '',
  });

  // Update profileData when user changes
  useEffect(() => {
    if (user) {
      setProfileData({
        name: user.name || '',
        email: user.email || '',
        role: user.role || '',
        avatar: user.avatar || '',
      });
    }
  }, [user]);

  const [securityData, setSecurityData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    twoFactorEnabled: true,
    smsMfaEnabled: false,
    countryCode: '+90',
    phoneNumber: '',
    sessionTimeout: 30,
  });

  const [appearanceData, setAppearanceData] = useState({
    theme: theme,
    density: 'comfortable',
    animations: true,
    reducedMotion: false,
  });

  const tabs = [
    { id: 'profile', label: 'Profile', icon: User },
    { id: 'security', label: 'Security', icon: Shield },
    { id: 'appearance', label: 'Appearance', icon: Palette },
  ];

  const handleProfileUpdate = useCallback(async () => {
    if (!user?.id) {
      alert('Kullanıcı bilgisi bulunamadı');
      return;
    }

    try {
      // Split name into firstName and lastName
      const nameParts = (profileData.name || '').trim().split(/\s+/);
      const firstName = nameParts[0] || '';
      const lastName = nameParts.slice(1).join(' ') || '';

      // Call backend API to update user
      const response = await api.put(endpoints.users.update(user.id), {
        email: profileData.email,
        firstName: firstName,
        lastName: lastName,
        role: profileData.role,
        // Don't update status, isVerified, mfaEnabled, phoneNumber, or userFile
      });

      // Update local state with response data
      const updatedUser = response.data;
      const updatedName = updatedUser.fullName || profileData.name;

      // Update auth store
      updateProfile({
        name: updatedName,
        email: updatedUser.email || profileData.email,
        role: updatedUser.role || profileData.role,
        avatar: profileData.avatar,
      });

      // Update local profileData state
      setProfileData(prev => ({
        ...prev,
        name: updatedName,
        email: updatedUser.email || prev.email,
        role: updatedUser.role || prev.role,
      }));

      alert('Profil başarıyla güncellendi!');
    } catch (error) {
      console.error('Profile update error:', error);
      alert(error.response?.data?.error || error.message || 'Profil güncellenirken bir hata oluştu');
    }
  }, [profileData, user, updateProfile]);

  const handlePasswordChange = useCallback(async () => {
    if (!user?.email) {
      alert('Kullanıcı bilgisi bulunamadı');
      return;
    }

    if (!securityData.currentPassword) {
      alert('Lütfen mevcut şifrenizi girin');
      return;
    }

    if (!securityData.newPassword) {
      alert('Lütfen yeni şifrenizi girin');
      return;
    }

    if (securityData.newPassword.length < 8) {
      alert('Yeni şifre en az 8 karakter olmalıdır');
      return;
    }

    if (securityData.newPassword !== securityData.confirmPassword) {
      alert('Yeni şifreler eşleşmiyor');
      return;
    }

    try {
      const url = endpoints.auth.changePassword;
      console.log('Changing password, URL:', url);
      console.log('Request data:', { email: user.email, currentPassword: '***', newPassword: '***' });

      const response = await api.post(url, {
        email: user.email,
        currentPassword: securityData.currentPassword,
        newPassword: securityData.newPassword,
      });

      console.log('Password change response:', response);

      alert('Şifre başarıyla değiştirildi!');

      // Clear password fields
      setSecurityData(prev => ({
        ...prev,
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      }));
    } catch (error) {
      console.error('Password change error:', error);
      alert(error.response?.data?.error || error.message || 'Şifre değiştirilirken bir hata oluştu');
    }
  }, [securityData, user]);

  const handleMFASetupComplete = useCallback(() => {
    setShowMFASetup(false);
    // Update user state to reflect MFA is enabled
    updateProfile({ ...user, totp_enabled: true });
  }, [user, updateProfile]);

  const handleThemeChange = useCallback((newTheme) => {
    setAppearanceData(prev => ({ ...prev, theme: newTheme }));
    setTheme(newTheme);
  }, [setTheme]);

  const ProfileTab = useMemo(() => (
    <div className="space-y-6">
      {/* Profile Information */}
      <Card>
        <CardHeader>
          <CardTitle>Profile Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-6">
            {/* Avatar */}
            <div className="flex items-center space-x-6">
              <div className="relative">
                <img
                  src={profileData.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=melisa'}
                  alt="Profile"
                  className="h-20 w-20 rounded-full border-2 border-surface-border"
                />
                <Button
                  variant="outline"
                  size="sm"
                  className="absolute -bottom-2 -right-2 h-8 w-8 rounded-full p-0"
                >
                  <Upload className="h-4 w-4" />
                </Button>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{profileData.name}</h3>
                <p className="text-gray-600 dark:text-surface-muted">{profileData.role}</p>
              </div>
            </div>

            {/* Form Fields */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Full Name</label>
                <Input
                  value={profileData.name}
                  onChange={(e) => setProfileData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="Enter your full name"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Email Address</label>
                <Input
                  type="email"
                  value={profileData.email}
                  onChange={(e) => setProfileData(prev => ({ ...prev, email: e.target.value }))}
                  placeholder="Enter your email"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Role</label>
                <Input
                  value={profileData.role}
                  onChange={(e) => setProfileData(prev => ({ ...prev, role: e.target.value }))}
                  placeholder="Enter your role"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Avatar URL</label>
                <Input
                  value={profileData.avatar}
                  onChange={(e) => setProfileData(prev => ({ ...prev, avatar: e.target.value }))}
                  placeholder="Enter avatar URL"
                />
              </div>
            </div>

            <Button onClick={handleProfileUpdate}>
              Update Profile
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Account Information */}
      <Card>
        <CardHeader>
          <CardTitle>Account Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex justify-between items-center p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Account Status</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">Your account status and verification</p>
              </div>
              <span className="px-3 py-1 bg-emerald-50 dark:bg-success/20 text-emerald-700 dark:text-success text-sm rounded-full">
                Active
              </span>
            </div>

            <div className="flex justify-between items-center p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Member Since</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">When you joined the platform</p>
              </div>
              <span className="text-gray-900 dark:text-white">January 2025</span>
            </div>

            <div className="flex justify-between items-center p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Last Login</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">Your most recent login</p>
              </div>
              <span className="text-gray-900 dark:text-white">Today at 10:30 AM</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  ), [profileData, handleProfileUpdate]);

  const SecurityTab = useMemo(() => (
    <div className="space-y-6">
      {/* Password Change */}
      <Card>
        <CardHeader>
          <CardTitle>Change Password</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Current Password</label>
              <div className="relative">
                <Input
                  type={showPassword ? 'text' : 'password'}
                  value={securityData.currentPassword}
                  onChange={(e) => setSecurityData(prev => ({ ...prev, currentPassword: e.target.value }))}
                  placeholder="Enter current password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 dark:text-surface-muted hover:text-gray-700 dark:hover:text-white"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">New Password</label>
              <Input
                type="password"
                value={securityData.newPassword}
                onChange={(e) => setSecurityData(prev => ({ ...prev, newPassword: e.target.value }))}
                placeholder="Enter new password"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Confirm New Password</label>
              <div className="relative">
                <Input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={securityData.confirmPassword}
                  onChange={(e) => setSecurityData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                  placeholder="Confirm new password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 dark:text-surface-muted hover:text-gray-700 dark:hover:text-white"
                >
                  {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button onClick={handlePasswordChange}>
              Change Password
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Two-Factor Authentication */}
      <Card>
        <CardHeader>
          <CardTitle>Two-Factor Authentication</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">2FA Status</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">Add an extra layer of security to your account</p>
              </div>
              <div className="flex items-center space-x-2">
                {user?.totp_enabled ? (
                  <>
                    <span className="px-3 py-1 bg-emerald-50 dark:bg-success/20 text-emerald-700 dark:text-success text-sm rounded-full">
                      Enabled
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={async () => {
                        try {
                          await api.post('/auth/mfa/disable', { username: user?.email });
                          updateProfile({ ...user, totp_enabled: false });
                        } catch (e) {
                          console.error(e);
                        }
                      }}
                    >
                      Configure
                    </Button>
                  </>
                ) : (
                  <>
                    <span className="px-3 py-1 bg-amber-50 dark:bg-warning/20 text-amber-700 dark:text-warning text-sm rounded-full">
                      Disabled
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setShowMFASetup(true)}
                    >
                      Enable MFA
                    </Button>
                  </>
                )}
              </div>
            </div>

            {/* SMS MFA */}
            <div className="p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">SMS MFA</p>
                  <p className="text-sm text-gray-600 dark:text-surface-muted">Telefon numaranıza SMS ile kod gönderilir</p>
                </div>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={securityData.smsMfaEnabled}
                    onChange={(e) => setSecurityData(prev => ({ ...prev, smsMfaEnabled: e.target.checked }))}
                    className="rounded border-gray-300 dark:border-surface-border bg-white dark:bg-surface-panel text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-900 dark:text-white">Aktif</span>
                </label>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
                <div>
                  <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Ülke</label>
                  <select
                    value={securityData.countryCode}
                    onChange={(e) => setSecurityData(prev => ({ ...prev, countryCode: e.target.value }))}
                    className="w-full px-3 py-2 bg-white dark:bg-surface-panel border border-gray-300 dark:border-surface-border rounded-lg text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="+90">Türkiye (+90)</option>
                    <option value="+1">ABD/Canada (+1)</option>
                    <option value="+44">Birleşik Krallık (+44)</option>
                    <option value="+49">Almanya (+49)</option>
                    <option value="+33">Fransa (+33)</option>
                    <option value="+34">İspanya (+34)</option>
                    <option value="+39">İtalya (+39)</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Telefon Numarası</label>
                  <Input
                    type="tel"
                    inputMode="numeric"
                    autoComplete="tel"
                    placeholder="555XXXXXXX (başında 0 yok)"
                    value={securityData.phoneNumber}
                    ref={phoneRef}
                    onChange={(e) => {
                      const next = e.target.value;
                      setSecurityData(prev => ({ ...prev, phoneNumber: next }));
                      // Odak kaybını engelle
                      requestAnimationFrame(() => phoneRef.current && phoneRef.current.focus());
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                      }
                    }}
                  />
                </div>
                <p className="text-sm text-gray-600 dark:text-surface-muted">
                  SMS tabanlı MFA şu anda desteklenmiyor.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Session Timeout</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">Automatically log out after inactivity</p>
              </div>
              <select
                value={securityData.sessionTimeout}
                onChange={(e) => setSecurityData(prev => ({ ...prev, sessionTimeout: e.target.value }))}
                className="px-3 py-2 bg-white dark:bg-surface-panel border border-gray-300 dark:border-surface-border rounded-lg text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value={15}>15 minutes</option>
                <option value={30}>30 minutes</option>
                <option value={60}>1 hour</option>
                <option value={120}>2 hours</option>
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Security Log */}
      <Card>
        <CardHeader>
          <CardTitle>Security Log</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Shield className="h-5 w-5 text-emerald-600 dark:text-success" />
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">Password changed</p>
                  <p className="text-sm text-gray-600 dark:text-surface-muted">Today at 9:15 AM</p>
                </div>
              </div>
              <span className="text-xs text-gray-600 dark:text-surface-muted">IP: 192.168.1.100</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Globe className="h-5 w-5 text-blue-600 dark:text-info" />
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">Login from new device</p>
                  <p className="text-sm text-gray-600 dark:text-surface-muted">Yesterday at 2:30 PM</p>
                </div>
              </div>
              <span className="text-xs text-gray-600 dark:text-surface-muted">IP: 203.0.113.45</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  ), [securityData, showPassword, showConfirmPassword, user, updateProfile, handlePasswordChange]);

  const AppearanceTab = useMemo(() => (
    <div className="space-y-6">
      {/* Theme Settings */}
      <Card>
        <CardHeader>
          <CardTitle>Theme & Appearance</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-6">
            {/* Theme Selection */}
            <div>
              <h4 className="font-medium text-gray-900 dark:text-white mb-3">Theme</h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div
                  className={cn(
                    'p-4 border rounded-lg cursor-pointer transition-all',
                    appearanceData.theme === 'dark'
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-gray-300 dark:border-surface-border hover:border-primary-400 bg-white dark:bg-surface-panel/30'
                  )}
                  onClick={() => handleThemeChange('dark')}
                >
                  <div className="flex items-center space-x-3">
                    <Moon className={cn(
                      'h-5 w-5',
                      appearanceData.theme === 'dark'
                        ? 'text-primary-600 dark:text-primary-400'
                        : 'text-gray-600 dark:text-primary-400'
                    )} />
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">Dark</p>
                      <p className="text-sm text-gray-600 dark:text-surface-muted">Default dark theme</p>
                    </div>
                  </div>
                </div>

                <div
                  className={cn(
                    'p-4 border rounded-lg cursor-pointer transition-all',
                    appearanceData.theme === 'light'
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-gray-300 dark:border-surface-border hover:border-primary-400 bg-white dark:bg-surface-panel/30'
                  )}
                  onClick={() => handleThemeChange('light')}
                >
                  <div className="flex items-center space-x-3">
                    <Sun className={cn(
                      'h-5 w-5',
                      appearanceData.theme === 'light'
                        ? 'text-primary-600 dark:text-primary-400'
                        : 'text-gray-600 dark:text-primary-400'
                    )} />
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">Light</p>
                      <p className="text-sm text-gray-600 dark:text-surface-muted">Light theme</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Other Settings */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Density</label>
                <select
                  value={appearanceData.density}
                  onChange={(e) => setAppearanceData(prev => ({ ...prev, density: e.target.value }))}
                  className="w-full px-3 py-2 bg-white dark:bg-surface-panel border border-gray-300 dark:border-surface-border rounded-lg text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="compact">Compact</option>
                  <option value="comfortable">Comfortable</option>
                  <option value="spacious">Spacious</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Animations</label>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="animations"
                    checked={appearanceData.animations}
                    onChange={(e) => setAppearanceData(prev => ({ ...prev, animations: e.target.checked }))}
                    className="rounded border-gray-300 dark:border-surface-border bg-white dark:bg-surface-panel text-primary-600 focus:ring-primary-500"
                  />
                  <label htmlFor="animations" className="text-gray-900 dark:text-white">Enable animations</label>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Sidebar Settings */}
      <Card>
        <CardHeader>
          <CardTitle>Layout Settings</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Sidebar Collapsed</p>
                <p className="text-sm text-gray-600 dark:text-surface-muted">Collapse the sidebar by default</p>
              </div>
              <div className="flex items-center space-x-3">
                <span className="text-sm text-gray-600 dark:text-surface-muted">
                  {sidebarCollapsed ? 'Collapsed' : 'Expanded'}
                </span>
                <Button variant="outline" size="sm" onClick={toggleSidebar}>
                  Toggle
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  ), [appearanceData, sidebarCollapsed, handleThemeChange, toggleSidebar]);

  const renderTabContent = () => {
    switch (activeTab) {
      case 'profile': return ProfileTab;
      case 'security': return SecurityTab;
      case 'appearance': return AppearanceTab;
      default: return ProfileTab;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Settings</h1>
        <p className="text-gray-600 dark:text-surface-muted">Manage your account and application preferences</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Sidebar Navigation */}
        <div className="lg:col-span-1">
          <Card>
            <CardContent className="p-0">
              <nav className="space-y-1">
                {tabs.map((tab) => {
                  const IconComponent = tab.icon;
                  return (
                    <button
                      key={tab.id}
                      onClick={() => setActiveTab(tab.id)}
                      className={cn(
                        'w-full flex items-center space-x-3 px-4 py-3 text-left text-sm font-medium rounded-lg transition-colors',
                        activeTab === tab.id
                          ? 'bg-primary-600 text-white'
                          : 'text-gray-700 dark:text-surface-muted hover:bg-gray-100 dark:hover:bg-surface-panel hover:text-gray-900 dark:hover:text-white'
                      )}
                    >
                      <IconComponent className="h-5 w-5" />
                      <span>{tab.label}</span>
                    </button>
                  );
                })}
              </nav>
            </CardContent>
          </Card>
        </div>

        {/* Main Content */}
        <div className="lg:col-span-3">
          {renderTabContent()}
        </div>
      </div>

      {/* MFA Setup Modal */}
      {showMFASetup && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <MFASetup
            onSetupComplete={handleMFASetupComplete}
            onCancel={() => setShowMFASetup(false)}
          />
        </div>
      )}
    </div>
  );
};

export default Settings;
