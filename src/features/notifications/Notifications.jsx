import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { Bell, AlertTriangle, Shield, Search, CheckCircle, X, Settings, Volume2, VolumeX, Mail, Smartphone } from 'lucide-react';
import { cn } from '../../lib/utils';

const Notifications = () => {
  const [activeTab, setActiveTab] = useState('all');
  const [showPreferences, setShowPreferences] = useState(false);

  // Mock notifications data
  const notifications = [
    {
      id: 1,
      type: 'security-alert',
      title: 'High-risk domain detected',
      message: 'Domain "malicious-site.com" has been flagged as high-risk with multiple threat indicators.',
      severity: 'high',
      timestamp: '2024-01-15T10:30:00Z',
      read: false,
      category: 'security',
      actions: ['View Details', 'Mark as False Positive'],
    },
    {
      id: 2,
      type: 'scan-complete',
      title: 'Domain scan completed',
      message: 'Security scan for "google.com" has completed. 12 findings detected, risk score: 3/10.',
      severity: 'low',
      timestamp: '2024-01-15T09:15:00Z',
      read: true,
      category: 'scan',
      actions: ['View Report', 'Download Results'],
    },
    {
      id: 3,
      type: 'breach-alert',
      title: 'New data breach detected',
      message: 'Email "test@example.com" found in 3 new data breaches. Immediate action recommended.',
      severity: 'medium',
      timestamp: '2024-01-15T08:45:00Z',
      read: false,
      category: 'breach',
      actions: ['View Details', 'Check Other Emails'],
    },
    {
      id: 4,
      type: 'system-update',
      title: 'System maintenance scheduled',
      message: 'Scheduled maintenance will begin at 02:00 UTC. Expected downtime: 30 minutes.',
      severity: 'info',
      timestamp: '2024-01-15T07:00:00Z',
      read: true,
      category: 'system',
      actions: ['View Schedule', 'Reschedule'],
    },
    {
      id: 5,
      type: 'threat-intel',
      title: 'New threat intelligence',
      message: 'New threat indicators added to database. 15 new IP addresses flagged.',
      severity: 'medium',
      timestamp: '2024-01-14T16:20:00Z',
      read: true,
      category: 'intelligence',
      actions: ['View Indicators', 'Update Rules'],
    },
  ];

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

  const markAsRead = (id) => {
    // In a real app, this would update the backend
    console.log('Marking notification as read:', id);
  };

  const markAllAsRead = () => {
    // In a real app, this would update the backend
    console.log('Marking all notifications as read');
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
                  <Volume2 className="h-4 w-4 text-success" />
                  <span className="text-success text-sm">Enabled</span>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-surface-panel/50 rounded-lg">
                <div>
                  <p className="font-medium text-white">Sound Alerts</p>
                  <p className="text-sm text-surface-muted">Play sound for important notifications</p>
                </div>
                <div className="flex items-center space-x-2">
                  <Volume2 className="h-4 w-4 text-success" />
                  <span className="text-success text-sm">Enabled</span>
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
                      defaultChecked
                      className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
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
                  <CheckCircle className="h-4 w-4 text-success" />
                  <span className="text-success text-sm">Enabled</span>
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
                    defaultChecked
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
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
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
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
                <select className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent">
                  <option value="daily">Daily</option>
                  <option value="weekly">Weekly</option>
                  <option value="monthly">Monthly</option>
                  <option value="never">Never</option>
                </select>
              </div>
            </div>
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
          <Button variant="outline" onClick={markAllAsRead}>
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
          { id: 'scan', label: 'Scans', count: notifications.filter(n => n.category === 'scan').length },
          { id: 'breach', label: 'Breaches', count: notifications.filter(n => n.category === 'breach').length },
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
            {filteredNotifications.length === 0 ? (
              <div className="text-center py-8">
                <Bell className="h-12 w-12 text-surface-muted mx-auto mb-4" />
                <p className="text-surface-muted">No notifications found</p>
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
                            <Button key={idx} variant="outline" size="sm">
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

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Bell className="h-6 w-6" />
              <span>Test Notifications</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Settings className="h-6 w-6" />
              <span>Notification Rules</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Mail className="h-6 w-6" />
              <span>Email Templates</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Notifications;
