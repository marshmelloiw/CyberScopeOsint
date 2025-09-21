import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { User, Sun, Moon, Shield, Globe, Bell, Key, Building, Palette, Lock, Eye, EyeOff, Download, Upload } from 'lucide-react';
import { cn } from '../../lib/utils';
import useUIStore from '../../store/ui';
import useAuthStore from '../../store/auth';
import MFASetup from '../../components/auth/MFASetup';

const Settings = () => {
  const [activeTab, setActiveTab] = useState('profile');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [showMFASetup, setShowMFASetup] = useState(false);
  
  const { theme, toggleTheme, sidebarCollapsed, toggleSidebar } = useUIStore();
  const { user, updateProfile } = useAuthStore();

  const [profileData, setProfileData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    role: user?.role || '',
    avatar: user?.avatar || '',
  });

  const [securityData, setSecurityData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    twoFactorEnabled: true,
    sessionTimeout: 30,
  });

  const [organizationData, setOrganizationData] = useState({
    name: 'CyberScope Security',
    domain: 'cyberscope.com',
    industry: 'Cybersecurity',
    size: '50-100',
    timezone: 'Europe/Istanbul',
    language: 'tr',
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
    { id: 'organization', label: 'Organization', icon: Building },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'integrations', label: 'Integrations', icon: Globe },
  ];

  const handleProfileUpdate = () => {
    updateProfile(profileData);
    console.log('Profile updated:', profileData);
  };

  const handlePasswordChange = () => {
    if (securityData.newPassword !== securityData.confirmPassword) {
      alert('Passwords do not match');
      return;
    }
    console.log('Password changed');
  };

  const handleMFASetupComplete = () => {
    setShowMFASetup(false);
    // Update user state to reflect MFA is enabled
    updateProfile({ ...user, totp_enabled: true });
  };

  const handleThemeChange = (newTheme) => {
    setAppearanceData(prev => ({ ...prev, theme: newTheme }));
    toggleTheme();
  };

  const ProfileTab = () => (
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
                <h3 className="text-lg font-semibold text-white">{profileData.name}</h3>
                <p className="text-surface-muted">{profileData.role}</p>
              </div>
            </div>

            {/* Form Fields */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-white mb-2">Full Name</label>
                <Input
                  value={profileData.name}
                  onChange={(e) => setProfileData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="Enter your full name"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-white mb-2">Email Address</label>
                <Input
                  type="email"
                  value={profileData.email}
                  onChange={(e) => setProfileData(prev => ({ ...prev, email: e.target.value }))}
                  placeholder="Enter your email"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-white mb-2">Role</label>
                <Input
                  value={profileData.role}
                  onChange={(e) => setProfileData(prev => ({ ...prev, role: e.target.value }))}
                  placeholder="Enter your role"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-white mb-2">Avatar URL</label>
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
            <div className="flex justify-between items-center p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">Account Status</p>
                <p className="text-sm text-surface-muted">Your account status and verification</p>
              </div>
              <span className="px-3 py-1 bg-success/20 text-success text-sm rounded-full">
                Active
              </span>
            </div>
            
            <div className="flex justify-between items-center p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">Member Since</p>
                <p className="text-sm text-surface-muted">When you joined the platform</p>
              </div>
              <span className="text-white">January 2024</span>
            </div>
            
            <div className="flex justify-between items-center p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">Last Login</p>
                <p className="text-sm text-surface-muted">Your most recent login</p>
              </div>
              <span className="text-white">Today at 10:30 AM</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const SecurityTab = () => (
    <div className="space-y-6">
      {/* Password Change */}
      <Card>
        <CardHeader>
          <CardTitle>Change Password</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-white mb-2">Current Password</label>
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
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-muted hover:text-white"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">New Password</label>
              <Input
                type="password"
                value={securityData.newPassword}
                onChange={(e) => setSecurityData(prev => ({ ...prev, newPassword: e.target.value }))}
                placeholder="Enter new password"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Confirm New Password</label>
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
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-muted hover:text-white"
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
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">2FA Status</p>
                <p className="text-sm text-surface-muted">Add an extra layer of security to your account</p>
              </div>
              <div className="flex items-center space-x-2">
                {user?.totp_enabled ? (
                  <>
                    <span className="px-3 py-1 bg-success/20 text-success text-sm rounded-full">
                      Enabled
                    </span>
                    <Button variant="outline" size="sm">
                      Configure
                    </Button>
                  </>
                ) : (
                  <>
                    <span className="px-3 py-1 bg-warning/20 text-warning text-sm rounded-full">
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
            
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">Session Timeout</p>
                <p className="text-sm text-surface-muted">Automatically log out after inactivity</p>
              </div>
              <select
                value={securityData.sessionTimeout}
                onChange={(e) => setSecurityData(prev => ({ ...prev, sessionTimeout: e.target.value }))}
                className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
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
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Shield className="h-5 w-5 text-success" />
                <div>
                  <p className="font-medium text-white">Password changed</p>
                  <p className="text-sm text-surface-muted">Today at 9:15 AM</p>
                </div>
              </div>
              <span className="text-xs text-surface-muted">IP: 192.168.1.100</span>
            </div>
            
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Globe className="h-5 w-5 text-info" />
                <div>
                  <p className="font-medium text-white">Login from new device</p>
                  <p className="text-sm text-surface-muted">Yesterday at 2:30 PM</p>
                </div>
              </div>
              <span className="text-xs text-surface-muted">IP: 203.0.113.45</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const AppearanceTab = () => (
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
              <h4 className="font-medium text-white mb-3">Theme</h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div
                  className={cn(
                    'p-4 border rounded-lg cursor-pointer transition-all',
                    appearanceData.theme === 'dark'
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-border hover:border-primary-400'
                  )}
                  onClick={() => handleThemeChange('dark')}
                >
                  <div className="flex items-center space-x-3">
                    <Moon className="h-5 w-5 text-primary-400" />
                    <div>
                      <p className="font-medium text-white">Dark</p>
                      <p className="text-sm text-surface-muted">Default dark theme</p>
                    </div>
                  </div>
                </div>
                
                <div
                  className={cn(
                    'p-4 border rounded-lg cursor-pointer transition-all',
                    appearanceData.theme === 'light'
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-border hover:border-primary-400'
                  )}
                  onClick={() => handleThemeChange('light')}
                >
                  <div className="flex items-center space-x-3">
                    <Sun className="h-5 w-5 text-primary-400" />
                    <div>
                      <p className="font-medium text-white">Light</p>
                      <p className="text-sm text-surface-muted">Light theme</p>
                    </div>
                  </div>
                </div>
                
                <div
                  className={cn(
                    'p-4 border rounded-lg cursor-pointer transition-all',
                    appearanceData.theme === 'auto'
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-border hover:border-primary-400'
                  )}
                  onClick={() => handleThemeChange('auto')}
                >
                  <div className="flex items-center space-x-3">
                    <Palette className="h-5 w-5 text-primary-400" />
                    <div>
                      <p className="font-medium text-white">Auto</p>
                      <p className="text-sm text-surface-muted">Follow system</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Other Settings */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-white mb-2">Density</label>
                <select
                  value={appearanceData.density}
                  onChange={(e) => setAppearanceData(prev => ({ ...prev, density: e.target.value }))}
                  className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="compact">Compact</option>
                  <option value="comfortable">Comfortable</option>
                  <option value="spacious">Spacious</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-white mb-2">Animations</label>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="animations"
                    checked={appearanceData.animations}
                    onChange={(e) => setAppearanceData(prev => ({ ...prev, animations: e.target.checked }))}
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                  />
                  <label htmlFor="animations" className="text-white">Enable animations</label>
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
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div>
                <p className="font-medium text-white">Sidebar Collapsed</p>
                <p className="text-sm text-surface-muted">Collapse the sidebar by default</p>
              </div>
              <div className="flex items-center space-x-3">
                <span className="text-sm text-surface-muted">
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
  );

  const OrganizationTab = () => (
    <div className="space-y-6">
      {/* Organization Information */}
      <Card>
        <CardHeader>
          <CardTitle>Organization Details</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white mb-2">Organization Name</label>
              <Input
                value={organizationData.name}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Enter organization name"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Domain</label>
              <Input
                value={organizationData.domain}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, domain: e.target.value }))}
                placeholder="Enter domain"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Industry</label>
              <select
                value={organizationData.industry}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, industry: e.target.value }))}
                className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="Cybersecurity">Cybersecurity</option>
                <option value="Finance">Finance</option>
                <option value="Healthcare">Healthcare</option>
                <option value="Technology">Technology</option>
                <option value="Other">Other</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Company Size</label>
              <select
                value={organizationData.size}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, size: e.target.value }))}
                className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="1-10">1-10 employees</option>
                <option value="11-50">11-50 employees</option>
                <option value="50-100">50-100 employees</option>
                <option value="100-500">100-500 employees</option>
                <option value="500+">500+ employees</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Timezone</label>
              <select
                value={organizationData.timezone}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, timezone: e.target.value }))}
                className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="Europe/Istanbul">Europe/Istanbul (UTC+3)</option>
                <option value="UTC">UTC (UTC+0)</option>
                <option value="America/New_York">America/New_York (UTC-5)</option>
                <option value="Europe/London">Europe/London (UTC+0)</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Language</label>
              <select
                value={organizationData.language}
                onChange={(e) => setOrganizationData(prev => ({ ...prev, language: e.target.value }))}
                className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="tr">Türkçe</option>
                <option value="en">English</option>
              </select>
            </div>
          </div>

          <div className="mt-6">
            <Button>
              Update Organization
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const NotificationsTab = () => (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Notification Preferences</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-surface-muted">
            Notification settings are managed in the Notifications page. 
            <Button variant="link" className="p-0 h-auto text-primary">
              Go to Notifications
            </Button>
          </p>
        </CardContent>
      </Card>
    </div>
  );

  const IntegrationsTab = () => (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>API Integrations</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Key className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium text-white">API Keys</p>
                  <p className="text-sm text-surface-muted">Manage your API keys and integrations</p>
                </div>
              </div>
              <Button variant="outline" size="sm">
                Manage Keys
              </Button>
            </div>
            
            <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Globe className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium text-white">Webhooks</p>
                  <p className="text-sm text-surface-muted">Configure webhook endpoints</p>
                </div>
              </div>
              <Button variant="outline" size="sm">
                Configure
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const renderTabContent = () => {
    switch (activeTab) {
      case 'profile': return <ProfileTab />;
      case 'security': return <SecurityTab />;
      case 'appearance': return <AppearanceTab />;
      case 'organization': return <OrganizationTab />;
      case 'notifications': return <NotificationsTab />;
      case 'integrations': return <IntegrationsTab />;
      default: return <ProfileTab />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Settings</h1>
        <p className="text-surface-muted">Manage your account and application preferences</p>
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
                          : 'text-surface-muted hover:bg-surface-panel hover:text-white'
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
