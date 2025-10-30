import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import StatWidget from '../../components/common/StatWidget';
import RiskBadge from '../../components/common/RiskBadge';
import {
  AlertTriangle,
  Shield,
  Search,
  FileSearch,
} from 'lucide-react';
import { cn } from '../../lib/utils';

const Dashboard = () => {
  // Mock data - in real app this would come from API
  const stats = [
    {
      title: 'Open Alerts',
      value: '12',
      change: '+3',
      changeType: 'negative',
      icon: AlertTriangle,
      trend: { direction: 'up', value: '+25%' },
    },
    {
      title: 'Avg. Risk Score',
      value: '4.2',
      change: '-0.8',
      changeType: 'positive',
      icon: Shield,
      trend: { direction: 'down', value: '-16%' },
    },
    {
      title: 'Active Scans',
      value: '5',
      change: '+2',
      changeType: 'neutral',
      icon: Search,
      trend: { direction: 'up', value: '+40%' },
    },
    {
      title: 'New Findings (24h)',
      value: '28',
      change: '+12',
      changeType: 'negative',
      icon: FileSearch,
      trend: { direction: 'up', value: '+75%' },
    },
  ];

  const recentAlerts = [
    {
      id: 1,
      severity: 'high',
      entity: 'google.com',
      source: 'VirusTotal',
      time: '2 hours ago',
      description: 'Malware detection in domain scan',
    },
    {
      id: 2,
      severity: 'medium',
      entity: '192.168.1.100',
      source: 'Shodan',
      time: '4 hours ago',
      description: 'Open port 22 detected',
    },
    {
      id: 3,
      severity: 'low',
      entity: 'test@example.com',
      source: 'HaveIBeenPwned',
      time: '6 hours ago',
      description: 'Email found in data breach',
    },
  ];

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'high':
        return 'severity-high';
      case 'medium':
        return 'severity-medium';
      case 'low':
        return 'severity-low';
      default:
        return 'severity-low';
    }
  };

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Dashboard</h1>
        <p className="text-surface-muted">Welcome back! Here's what's happening with your security posture.</p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat, index) => (
          <StatWidget key={index} {...stat} />
        ))}
      </div>

      {/* Main content grid */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Recent alerts */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Recent Alerts</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {recentAlerts.map((alert) => (
                  <div
                    key={alert.id}
                    className="flex items-center justify-between rounded-lg border border-surface-border p-4 hover:bg-surface-panel/50 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={cn('h-3 w-3 rounded-full', getSeverityColor(alert.severity))} />
                      <div>
                        <p className="font-medium text-white">{alert.entity}</p>
                        <p className="text-sm text-surface-muted">{alert.description}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm text-surface-muted">{alert.source}</p>
                      <p className="text-xs text-surface-muted">{alert.time}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-4 text-center">
                <button className="text-sm text-primary-500 hover:text-primary-400 transition-colors">
                  View all alerts →
                </button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Quick actions */}
        <div>
          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <button className="w-full rounded-lg bg-primary-600 p-3 text-left text-white hover:bg-primary-700 transition-colors">
                <div className="flex items-center space-x-2">
                  <Search className="h-5 w-5" />
                  <span>New Scan</span>
                </div>
              </button>
              <button className="w-full rounded-lg bg-surface-panel p-3 text-left text-white hover:bg-surface-border transition-colors border border-surface-border">
                <div className="flex items-center space-x-2">
                  <FileSearch className="h-5 w-5" />
                  <span>Generate Report</span>
                </div>
              </button>
              <button className="w-full rounded-lg bg-surface-panel p-3 text-left text-white hover:bg-surface-border transition-colors border border-surface-border">
                <div className="flex items-center space-x-2">
                  <Shield className="h-5 w-5" />
                  <span>Security Check</span>
                </div>
              </button>
            </CardContent>
          </Card>

          {/* System status */}
          <Card className="mt-6">
            <CardHeader>
              <CardTitle>System Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-surface-muted">API Status</span>
                  <span className="flex items-center space-x-2">
                    <div className="h-2 w-2 rounded-full bg-success"></div>
                    <span className="text-sm text-success">Online</span>
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-surface-muted">Database</span>
                  <span className="flex items-center space-x-2">
                    <div className="h-2 w-2 rounded-full bg-success"></div>
                    <span className="text-sm text-success">Connected</span>
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-surface-muted">External APIs</span>
                  <span className="flex items-center space-x-2">
                    <div className="h-2 w-2 rounded-full bg-success"></div>
                    <span className="text-sm text-success">All Active</span>
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Charts section - placeholder for now */}
      <Card>
        <CardHeader>
          <CardTitle>Security Trends</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-64 flex items-center justify-center text-surface-muted">
            <p>Charts will be implemented with Recharts library</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Dashboard;
