import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { Bell, AlertTriangle, Shield, Search, CheckCircle, X, Settings, Volume2, VolumeX, Mail, Smartphone } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';
import useAuthStore from '../../store/auth';

const Notifications = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');
  const [showPreferences, setShowPreferences] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuthStore();

  // Notification preferences state
  const [preferences, setPreferences] = useState({
    enableNotifications: true,
    soundAlerts: true,
    categoryPreferences: {
      security: true,
      scan: true,
      breach: true,
      system: true,
      intelligence: true,
    },
    inAppNotifications: true,
    emailNotifications: true,
    pushNotifications: false,
    digestFrequency: 'daily',
  });
  const [savingPreferences, setSavingPreferences] = useState(false);

  // Fetch notifications from API
  useEffect(() => {
    fetchNotifications();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  // Fetch preferences when preferences panel is opened
  useEffect(() => {
    if (showPreferences && user?.id) {
      fetchPreferences();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showPreferences, user?.id]);

  const fetchPreferences = async () => {
    try {
      const userId = user?.id;
      if (!userId) return;

      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId;
      const response = await api.get(endpoints.notifications.preferences, {
        params: { userId: numericUserId }
      });

      if (response.data) {
        setPreferences(prev => ({
          ...prev,
          ...response.data,
          // Ensure categoryPreferences is properly merged
          categoryPreferences: response.data.categoryPreferences || prev.categoryPreferences,
        }));
      }
    } catch (error) {
      console.error('Error fetching preferences:', error);
      // Keep default preferences on error
    }
  };

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const userId = user?.id;
      console.log('Fetching notifications for userId:', userId, 'user:', user);
      
      if (!userId) {
        console.warn('No userId found, cannot fetch notifications');
        setNotifications([]);
        setLoading(false);
        return;
      }

      // Ensure userId is a number, not a string
      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId;
      console.log('Making API call with userId:', numericUserId, 'type:', typeof numericUserId);
      
      const response = await api.get(endpoints.notifications.list, {
        params: { userId: numericUserId }
      });
      
      console.log('Notifications API response:', response.data);
      
      if (response.data && response.data.notifications) {
        // Map API response to component format
        const mappedNotifications = response.data.notifications.map(notif => {
          console.log('Mapping notification:', notif);
          
          // Parse timestamp - handle both ISO string and LocalDateTime format
          let timestamp = notif.createdAt;
          if (timestamp && typeof timestamp === 'string') {
            // If it's already a string, use it directly
            // If it's in format "2024-01-15T10:30:00", add 'Z' for ISO
            if (!timestamp.includes('Z') && !timestamp.includes('+')) {
              timestamp = timestamp + 'Z';
            }
          } else if (!timestamp) {
            timestamp = new Date().toISOString();
          }
          
          return {
            id: notif.id,
      type: 'security-alert',
            title: `Yüksek Risk Skoru: ${notif.riskScore || 'N/A'}`,
            message: notif.message || `Risk skoru ${notif.riskScore || 'N/A'} tespit edildi`,
            severity: mapRiskLevelToSeverity(notif.riskLevel),
            timestamp: timestamp,
            read: notif.isRead || false,
      category: 'security',
            actions: ['Detayları Görüntüle'],
            riskScore: notif.riskScore,
            riskLevel: notif.riskLevel,
            scanId: notif.scanId,
            scanIdString: notif.scanIdString, // UUID string for navigation
          };
        });
        console.log('Mapped notifications:', mappedNotifications);
        setNotifications(mappedNotifications);
        // Dispatch event to update header count
        window.dispatchEvent(new CustomEvent('notificationUpdated'));
      } else {
        console.warn('No notifications in response:', response.data);
        setNotifications([]);
        // Dispatch event to update header count even if empty
        window.dispatchEvent(new CustomEvent('notificationUpdated'));
      }
    } catch (error) {
      console.error('Error fetching notifications:', error);
      console.error('Error details:', error.response?.data || error.message);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  const mapRiskLevelToSeverity = (riskLevel) => {
    if (!riskLevel) return 'medium';
    const level = riskLevel.toUpperCase();
    if (level === 'CRITICAL') return 'high';
    if (level === 'HIGH') return 'high';
    if (level === 'MEDIUM') return 'medium';
    return 'low';
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'high': return 'text-danger';
      case 'medium': return 'text-warning';
      case 'low': return 'text-success';
      case 'info': return 'text-info';
      default: return 'text-surface-muted';
    }
  };

  const getSeverityBadge = (severity) => {
    const colors = {
      high: 'bg-danger/20 text-danger border-danger/30',
      medium: 'bg-warning/20 text-warning border-warning/30',
      low: 'bg-success/20 text-success border-success/30',
      info: 'bg-info/20 text-info border-info/30',
    };
    
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${colors[severity] || 'bg-surface-muted/20 text-surface-muted border-surface-muted/30'}`}>
        {severity.toUpperCase()}
      </span>
    );
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'security-alert': return <AlertTriangle className="h-5 w-5 text-danger" />;
      case 'scan-complete': return <Search className="h-5 w-5 text-success" />;
      case 'breach-alert': return <Shield className="h-5 w-5 text-warning" />;
      case 'system-update': return <Settings className="h-5 w-5 text-info" />;
      case 'threat-intel': return <Bell className="h-5 w-5 text-primary" />;
      default: return <Bell className="h-5 w-5 text-surface-muted" />;
    }
  };

  const getCategoryColor = (category) => {
    switch (category) {
      case 'security': return 'bg-danger/10 border-danger/20';
      case 'scan': return 'bg-success/10 border-success/20';
      case 'breach': return 'bg-warning/10 border-warning/20';
      case 'system': return 'bg-info/10 border-info/20';
      case 'intelligence': return 'bg-primary/10 border-primary/20';
      default: return 'bg-surface-panel/50 border-surface-border';
    }
  };

  const filteredNotifications = notifications.filter(notification => {
    if (activeTab === 'all') return true;
    if (activeTab === 'unread') return !notification.read;
    return notification.category === activeTab;
  });

  const unreadCount = notifications.filter(n => !n.read).length;

  const markAsRead = async (id) => {
    try {
      await api.put(`/notifications/${id}/read`);
      // Update local state
      setNotifications(prev => 
        prev.map(notif => 
          notif.id === id ? { ...notif, read: true } : notif
        )
      );
      // Dispatch event to update header count
      window.dispatchEvent(new CustomEvent('notificationUpdated'));
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  };

  const markAllAsRead = async () => {
    try {
      const userId = user?.id;
      if (!userId) return;
      
      await api.put('/notifications/read-all', null, {
        params: { userId }
      });
      // Update local state
      setNotifications(prev => 
        prev.map(notif => ({ ...notif, read: true }))
      );
      // Dispatch event to update header count
      window.dispatchEvent(new CustomEvent('notificationUpdated'));
    } catch (error) {
      console.error('Error marking all notifications as read:', error);
    }
  };

  const savePreferences = async () => {
    try {
      setSavingPreferences(true);
      const userId = user?.id;
      if (!userId) {
        console.warn('No userId found, cannot save preferences');
        return;
      }

      const response = await api.put(endpoints.notifications.preferences, {
        userId: userId,
        ...preferences,
      });

      console.log('Preferences saved:', response.data);
      
      // Update state with saved preferences from response
      if (response.data) {
        setPreferences(prev => ({
          ...prev,
          enableNotifications: response.data.enableNotifications ?? prev.enableNotifications,
          soundAlerts: response.data.soundAlerts ?? prev.soundAlerts,
          inAppNotifications: response.data.inAppNotifications ?? prev.inAppNotifications,
          emailNotifications: response.data.emailNotifications ?? prev.emailNotifications,
          pushNotifications: response.data.pushNotifications ?? prev.pushNotifications,
          digestFrequency: response.data.digestFrequency ?? prev.digestFrequency,
          categoryPreferences: response.data.categoryPreferences || prev.categoryPreferences,
        }));
      }
      
      // Close preferences panel after successful save
      setShowPreferences(false);
      
      // Show success message (you can add a toast notification here)
      alert('Ayarlar başarıyla kaydedildi');
    } catch (error) {
      console.error('Error saving preferences:', error);
      // Show error message
      alert('Ayarlar kaydedilirken bir hata oluştu: ' + (error.response?.data?.error || error.message));
    } finally {
      setSavingPreferences(false);
    }
  };

  const NotificationPreferences = () => (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Settings className="h-5 w-5" />
          <span>Notification Preferences</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {/* General Settings */}
          <div>
            <h4 className="font-medium text-white mb-3">General Settings</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div>
                  <p className="font-medium text-white">Enable Notifications</p>
                  <p className="text-sm text-surface-muted">Receive notifications in the application</p>
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setPreferences(prev => ({ ...prev, enableNotifications: !prev.enableNotifications }))}
                    className={cn(
                      "relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2",
                      preferences.enableNotifications ? "bg-primary-600" : "bg-surface-border"
                    )}
                  >
                    <span
                      className={cn(
                        "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                        preferences.enableNotifications ? "translate-x-6" : "translate-x-1"
                      )}
                    />
                  </button>
                  {preferences.enableNotifications ? (
                  <Volume2 className="h-4 w-4 text-success" />
                  ) : (
                    <VolumeX className="h-4 w-4 text-surface-muted" />
                  )}
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div>
                  <p className="font-medium text-white">Sound Alerts</p>
                  <p className="text-sm text-surface-muted">Play sound for important notifications</p>
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setPreferences(prev => ({ ...prev, soundAlerts: !prev.soundAlerts }))}
                    className={cn(
                      "relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2",
                      preferences.soundAlerts ? "bg-primary-600" : "bg-surface-border"
                    )}
                  >
                    <span
                      className={cn(
                        "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                        preferences.soundAlerts ? "translate-x-6" : "translate-x-1"
                      )}
                    />
                  </button>
                  {preferences.soundAlerts ? (
                  <Volume2 className="h-4 w-4 text-success" />
                  ) : (
                    <VolumeX className="h-4 w-4 text-surface-muted" />
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Category Preferences */}
          <div>
            <h4 className="font-medium text-white mb-3">Category Preferences</h4>
            <div className="space-y-3">
              {['security', 'scan', 'breach', 'system', 'intelligence'].map((category) => (
                <div key={category} className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                  <div>
                    <p className="font-medium text-white capitalize">{category} Alerts</p>
                    <p className="text-sm text-surface-muted">
                      Receive notifications for {category} related events
                    </p>
                  </div>
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id={category}
                      checked={preferences.categoryPreferences[category] || false}
                      onChange={(e) => setPreferences(prev => ({
                        ...prev,
                        categoryPreferences: {
                          ...prev.categoryPreferences,
                          [category]: e.target.checked
                        }
                      }))}
                      className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500 cursor-pointer"
                    />
                    <label htmlFor={category} className="sr-only">Enable {category} notifications</label>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Delivery Methods */}
          <div>
            <h4 className="font-medium text-white mb-3">Delivery Methods</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <Bell className="h-5 w-5 text-primary" />
                  <div>
                    <p className="font-medium text-white">In-App Notifications</p>
                    <p className="text-sm text-surface-muted">Show notifications in the application</p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setPreferences(prev => ({ ...prev, inAppNotifications: !prev.inAppNotifications }))}
                    className={cn(
                      "relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2",
                      preferences.inAppNotifications ? "bg-primary-600" : "bg-surface-border"
                    )}
                  >
                    <span
                      className={cn(
                        "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                        preferences.inAppNotifications ? "translate-x-6" : "translate-x-1"
                      )}
                    />
                  </button>
                  {preferences.inAppNotifications ? (
                  <CheckCircle className="h-4 w-4 text-success" />
                  ) : (
                    <X className="h-4 w-4 text-surface-muted" />
                  )}
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <Mail className="h-5 w-5 text-primary" />
                  <div>
                    <p className="font-medium text-white">Email Notifications</p>
                    <p className="text-sm text-surface-muted">Send notifications via email</p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="email"
                    checked={preferences.emailNotifications}
                    onChange={(e) => setPreferences(prev => ({ ...prev, emailNotifications: e.target.checked }))}
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500 cursor-pointer"
                  />
                  <label htmlFor="email" className="sr-only">Enable email notifications</label>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <Smartphone className="h-5 w-5 text-primary" />
                  <div>
                    <p className="font-medium text-white">Push Notifications</p>
                    <p className="text-sm text-surface-muted">Send push notifications to mobile devices</p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="push"
                    checked={preferences.pushNotifications}
                    onChange={(e) => setPreferences(prev => ({ ...prev, pushNotifications: e.target.checked }))}
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500 cursor-pointer"
                  />
                  <label htmlFor="push" className="sr-only">Enable push notifications</label>
                </div>
              </div>
            </div>
          </div>

          {/* Frequency Settings */}
          <div>
            <h4 className="font-medium text-white mb-3">Frequency Settings</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div>
                  <p className="font-medium text-white">Digest Frequency</p>
                  <p className="text-sm text-surface-muted">How often to send notification digests</p>
                </div>
                <select 
                  value={preferences.digestFrequency}
                  onChange={(e) => setPreferences(prev => ({ ...prev, digestFrequency: e.target.value }))}
                  className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent cursor-pointer"
                >
                  <option value="daily">Daily</option>
                  <option value="weekly">Weekly</option>
                  <option value="monthly">Monthly</option>
                  <option value="never">Never</option>
                </select>
              </div>
            </div>
          </div>

          {/* Save Button */}
          <div className="flex justify-end pt-4 border-t border-surface-border">
            <Button 
              onClick={savePreferences}
              disabled={savingPreferences}
            >
              {savingPreferences ? 'Kaydediliyor...' : 'Kaydet'}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Notifications</h1>
          <p className="text-surface-muted">Manage your security alerts and notifications</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button 
            variant="outline" 
            onClick={markAllAsRead}
            disabled={unreadCount === 0}
          >
            <CheckCircle className="h-4 w-4 mr-2" />
            Mark All Read
          </Button>
          <Button onClick={() => setShowPreferences(!showPreferences)}>
            <Settings className="h-4 w-4 mr-2" />
            Preferences
          </Button>
        </div>
      </div>

      {/* Notification Preferences */}
      {showPreferences && <NotificationPreferences />}

      {/* Tabs */}
      <div className="flex space-x-1 bg-surface-panel/50 p-1 rounded-lg">
        {[
          { id: 'all', label: 'All', count: notifications.length },
          { id: 'unread', label: 'Unread', count: unreadCount },
          { id: 'security', label: 'Security', count: notifications.filter(n => n.category === 'security').length },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              'flex items-center space-x-2 px-4 py-2 rounded-md text-sm font-medium transition-colors',
              activeTab === tab.id
                ? 'bg-primary-600 text-white'
                : 'text-surface-muted hover:text-white hover:bg-surface-panel'
            )}
          >
            <span>{tab.label}</span>
            <span className="px-2 py-1 bg-surface-panel/50 rounded-full text-xs">
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Notifications list */}
      <Card>
        <CardHeader>
          <CardTitle>
            {activeTab === 'all' ? 'All Notifications' : 
             activeTab === 'unread' ? 'Unread Notifications' : 
             `${activeTab.charAt(0).toUpperCase() + activeTab.slice(1)} Notifications`}
            <span className="ml-2 text-surface-muted">({filteredNotifications.length})</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {loading ? (
              <div className="text-center py-8">
                <p className="text-surface-muted">Yükleniyor...</p>
              </div>
            ) : filteredNotifications.length === 0 ? (
              <div className="text-center py-8">
                <Bell className="h-12 w-12 text-surface-muted mx-auto mb-4" />
                <p className="text-surface-muted">Bildirim bulunamadı</p>
              </div>
            ) : (
              filteredNotifications.map((notification) => (
                <div
                  key={notification.id}
                  className={cn(
                    'p-4 border rounded-lg transition-colors',
                    notification.read 
                      ? 'border-surface-border/50 bg-surface-panel/30' 
                      : 'border-primary-500/30 bg-primary-500/10',
                    'hover:bg-surface-panel/50'
                  )}
                >
                  <div className="flex items-start space-x-4">
                    <div className="flex-shrink-0">
                      {getTypeIcon(notification.type)}
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between">
                        <div className="flex items-center space-x-3">
                          <h3 className={cn(
                            'text-lg font-semibold',
                            notification.read ? 'text-white' : 'text-primary-400'
                          )}>
                            {notification.title}
                          </h3>
                          {getSeverityBadge(notification.severity)}
                          {!notification.read && (
                            <span className="px-2 py-1 bg-primary-500 text-white text-xs rounded-full">
                              NEW
                            </span>
                          )}
                        </div>
                        
                        <div className="flex items-center space-x-2">
                          <span className="text-sm text-surface-muted">
                            {new Date(notification.timestamp).toLocaleDateString()}
                          </span>
                          {!notification.read && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => markAsRead(notification.id)}
                            >
                              <CheckCircle className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </div>
                      
                      <p className="text-surface-muted mt-2">{notification.message}</p>
                      
                      <div className="flex items-center space-x-4 mt-3">
                        <div className="flex space-x-2">
                          {notification.actions.map((action, idx) => (
                            <Button 
                              key={idx} 
                              variant="outline" 
                              size="sm"
                              onClick={() => {
                                if (action === 'Detayları Görüntüle' && notification.scanIdString) {
                                  navigate(`/dashboard/scans/${notification.scanIdString}`);
                                }
                              }}
                            >
                              {action}
                            </Button>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Notifications;
