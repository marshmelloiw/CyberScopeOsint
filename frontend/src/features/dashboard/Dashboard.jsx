import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import StatWidget from '../../components/common/StatWidget';
import RiskBadge from '../../components/common/RiskBadge';
import {
  AlertTriangle,
  Shield,
  Search,
  FileSearch,
  Loader2,
  Key,
  Settings as SettingsIcon,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';
import useAuthStore from '../../store/auth';
import { ROLE } from '../../constants/roles';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

const Dashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const role = user?.role ?? ROLE.VIEWER;
  const isAdmin = role === ROLE.ADMIN;
  const isAnalyst = role === ROLE.ANALYST;
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toolUsageData, setToolUsageData] = useState(null);
  const [userActivityData, setUserActivityData] = useState(null);
  const [scanStatusData, setScanStatusData] = useState(null);
  const [chartsLoading, setChartsLoading] = useState(true);

  useEffect(() => {
    fetchSummary();
    fetchChartData();
    
    // Auto-refresh every 30 seconds to get latest data (silent refresh - no loading states)
    const interval = setInterval(() => {
      // Save scroll position
      const scrollY = window.scrollY;
      
      // Silent refresh - update data without showing loading states
      fetchSummarySilent();
      fetchChartDataSilent();
      
      // Restore scroll position after a brief delay
      setTimeout(() => {
        window.scrollTo(0, scrollY);
      }, 100);
    }, 30000); // Refresh every 30 seconds (reduced frequency)
    
    return () => clearInterval(interval);
  }, []);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      setError(null);
      const { data } = await api.get(endpoints.dashboard.summary);
      setSummary(data);
    } catch (err) {
      const message = err?.response?.data?.error || 'Dashboard verileri yüklenemedi';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const fetchSummarySilent = async () => {
    try {
      // Silent refresh - don't set loading state
      const { data } = await api.get(endpoints.dashboard.summary);
      setSummary(data);
    } catch (err) {
      // Silent error - don't show error state on auto-refresh
      console.error('Silent refresh error:', err);
    }
  };


  const fetchChartData = async () => {
    try {
      setChartsLoading(true);
      const [toolsRes, usersRes, scansRes] = await Promise.all([
        api.get(endpoints.dashboard.charts.tools).catch(err => {
          console.error('Error fetching tools data:', err);
          return { data: { tools: [] } };
        }),
        api.get(endpoints.dashboard.charts.users).catch(err => {
          console.error('Error fetching users data:', err);
          return { data: { active: 0, inactive: 0, activePercentage: 0, inactivePercentage: 0 } };
        }),
        api.get(endpoints.dashboard.charts.scans).catch(err => {
          console.error('Error fetching scans data:', err);
          return { data: { statuses: [] } };
        }),
      ]);
      setToolUsageData(toolsRes.data || { tools: [] });
      setUserActivityData(usersRes.data || { active: 0, inactive: 0, activePercentage: 0, inactivePercentage: 0 });
      setScanStatusData(scansRes.data || { statuses: [] });
    } catch (err) {
      console.error('Error fetching chart data:', err);
      // Set default empty data
      setToolUsageData({ tools: [] });
      setUserActivityData({ active: 0, inactive: 0, activePercentage: 0, inactivePercentage: 0 });
      setScanStatusData({ statuses: [] });
    } finally {
      setChartsLoading(false);
    }
  };

  const fetchChartDataSilent = async () => {
    try {
      // Silent refresh - don't set loading state
      const [toolsRes, usersRes, scansRes] = await Promise.all([
        api.get(endpoints.dashboard.charts.tools).catch(err => {
          return { data: { tools: [] } };
        }),
        api.get(endpoints.dashboard.charts.users).catch(err => {
          return { data: { active: 0, inactive: 0, activePercentage: 0, inactivePercentage: 0 } };
        }),
        api.get(endpoints.dashboard.charts.scans).catch(err => {
          return { data: { statuses: [] } };
        }),
      ]);
      setToolUsageData(toolsRes.data || { tools: [] });
      setUserActivityData(usersRes.data || { active: 0, inactive: 0, activePercentage: 0, inactivePercentage: 0 });
      setScanStatusData(scansRes.data || { statuses: [] });
    } catch (err) {
      // Silent error - don't update state on error during auto-refresh
      console.error('Silent chart refresh error:', err);
    }
  };

  const numberFormatter = useMemo(() => new Intl.NumberFormat('tr-TR'), []);
  const riskFormatter = useMemo(() => new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 1, minimumFractionDigits: 1 }), []);

  const stats = useMemo(() => {
    if (!summary) {
      return [];
    }

    return [
      {
        title: 'Open Alerts',
        value: numberFormatter.format(summary.openAlerts ?? 0),
        icon: AlertTriangle,
      },
      {
        title: 'Avg. Risk Score',
        value: riskFormatter.format(summary.averageRiskScore ?? 0),
        icon: Shield,
      },
      {
        title: 'Active Scans',
        value: numberFormatter.format(summary.activeScans ?? 0),
        icon: Search,
      },
      {
        title: 'New Findings (24h)',
        value: numberFormatter.format(summary.newFindings24h ?? 0),
        icon: FileSearch,
      },
    ];
  }, [summary, numberFormatter, riskFormatter]);

  const recentAlerts = summary?.recentAlerts ?? [];
  const providerStatus = summary?.providers ?? {};

  const quickActions = useMemo(() => {
    const actions = [
      {
        key: 'reports',
        label: 'Raporları Görüntüle',
        description: 'Analiz raporlarına göz atın',
        icon: FileSearch,
        onClick: () => navigate('/dashboard/reports'),
        variant: 'secondary',
      },
    ];

    if (isAdmin || isAnalyst) {
      actions.unshift({
        key: 'new-scan',
        label: 'Yeni Tarama',
        description: 'Hemen yeni bir keşif başlatın',
        icon: Search,
        onClick: () => navigate('/dashboard/scans/new'),
        variant: 'primary',
      });
    }

    if (isAdmin) {
      actions.push({
        key: 'user-management',
        label: 'Kullanıcı Yönetimi',
        description: 'Rolleri ve erişimleri yönetin',
        icon: Shield,
        onClick: () => navigate('/dashboard/users'),
        variant: 'tertiary',
      });
      actions.push({
        key: 'settings',
        label: 'Sistem Ayarları',
        description: 'Platform yapılandırmasını düzenleyin',
        icon: SettingsIcon,
        onClick: () => navigate('/dashboard/settings'),
        variant: 'tertiary',
      });
    }

    if (role === ROLE.VIEWER) {
      actions.push({
        key: 'notifications',
        label: 'Bildirimler',
        description: 'Güncel bildirimleri inceleyin',
        icon: AlertTriangle,
        onClick: () => navigate('/dashboard/notifications'),
        variant: 'secondary',
      });
    }

    if (isAdmin && !actions.some((a) => a.key === 'apikeys')) {
      actions.push({
        key: 'apikeys',
        label: 'API Anahtarları',
        description: 'Harici servis entegrasyonlarını yönetin',
        icon: Key,
        onClick: () => navigate('/dashboard/apikeys'),
        variant: 'secondary',
      });
    }

    return actions;
  }, [isAdmin, isAnalyst, navigate, role]);

  const getSeverityColor = (severity = 'low') => {
    switch (severity.toLowerCase()) {
      case 'high':
      case 'critical':
        return 'severity-high';
      case 'medium':
        return 'severity-medium';
      case 'low':
      default:
        return 'severity-low';
    }
  };

  const formatRelativeTime = (isoString) => {
    if (!isoString) return '—';
    try {
      const date = new Date(isoString);
      const diffMs = Date.now() - date.getTime();
      const diffSeconds = Math.round(diffMs / 1000);
      const rtf = new Intl.RelativeTimeFormat('tr', { numeric: 'auto' });

      if (Math.abs(diffSeconds) < 60) {
        return rtf.format(-diffSeconds, 'second');
      }
      const diffMinutes = Math.round(diffSeconds / 60);
      if (Math.abs(diffMinutes) < 60) {
        return rtf.format(-diffMinutes, 'minute');
      }
      const diffHours = Math.round(diffMinutes / 60);
      if (Math.abs(diffHours) < 24) {
        return rtf.format(-diffHours, 'hour');
      }
      const diffDays = Math.round(diffHours / 24);
      return rtf.format(-diffDays, 'day');
    } catch (error) {
      return isoString;
    }
  };

  const systemStatusRows = useMemo(() => ([
    { label: 'API Status', active: providerStatus.apiOnline !== false },
    { label: 'Database', active: providerStatus.databaseConnected !== false },
    { label: 'Shodan', active: !!providerStatus.shodan },
    { label: 'VirusTotal', active: !!providerStatus.virusTotal },
    { label: 'HaveIBeenPwned', active: !!providerStatus.hibp },
    { label: 'OWASP ZAP', active: !!providerStatus.zap },
    { label: 'Gemini AI', active: !!providerStatus.gemini },
  ]), [providerStatus]);

  const getQuickActionClasses = (variant) => {
    switch (variant) {
      case 'primary':
        return 'bg-primary-600 text-white hover:bg-primary-700 dark:bg-primary-600 dark:text-white dark:hover:bg-primary-700';
      case 'secondary':
        return 'bg-surface-panel border border-surface-border text-gray-900 dark:text-white hover:bg-gray-50 dark:hover:bg-surface-border transition-all';
      case 'tertiary':
        return 'bg-surface-panel border border-primary-500/50 text-primary-700 dark:text-primary-200 hover:bg-primary-50 dark:hover:bg-primary-600/10 hover:border-primary-600 dark:hover:border-primary-500 transition-all';
      default:
        return 'bg-surface-panel border border-surface-border text-gray-900 dark:text-white hover:bg-gray-50 dark:hover:bg-surface-border transition-all';
    }
  };

  if (loading) {
    return (
      <div className="flex h-full min-h-[50vh] items-center justify-center">
        <div className="flex items-center space-x-3 text-surface-muted">
          <Loader2 className="h-6 w-6 animate-spin text-primary-500" />
          <span>Dashboard verileri yükleniyor...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full min-h-[50vh] items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-lg font-medium text-danger">{error}</p>
          <button
            onClick={fetchSummary}
            className="rounded-lg bg-primary-600 px-4 py-2 text-white hover:bg-primary-700 transition-colors"
          >
            Tekrar dene
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-white">Dashboard</h1>
        <p className="text-surface-muted">Güvenlik görünümünüzün güncel özeti.</p>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat, index) => (
          <StatWidget key={index} {...stat} />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Recent Alerts</CardTitle>
            </CardHeader>
            <CardContent>
              {recentAlerts.length === 0 ? (
                <div className="flex items-center justify-center py-10 text-surface-muted">
                  <p>Henüz uyarı bulunmuyor.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {recentAlerts.map((alert) => (
                    <div
                      key={alert.id}
                      onClick={() => alert.scanId ? navigate(`/dashboard/scans/${alert.scanId}`) : navigate('/notifications')}
                      className="flex items-center justify-between rounded-lg border border-surface-border p-4 hover:bg-surface-panel/50 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center space-x-3">
                        <div className={cn('h-3 w-3 rounded-full', getSeverityColor(alert.severity))} />
                        <div>
                          <p className="font-medium text-white">{alert.entity}</p>
                          <p className="text-sm text-surface-muted">{alert.description}</p>
                          <div className="mt-1 flex items-center space-x-2 text-xs text-surface-muted">
                            <span>{alert.provider}</span>
                            {alert.findings > 0 && <span>• {alert.findings} bulgu</span>}
                          </div>
                        </div>
                      </div>
                      <div className="text-right space-y-1">
                        {alert.riskScore != null && (
                          <RiskBadge risk={Number(alert.riskScore)} />
                        )}
                        <p className="text-xs text-surface-muted">{formatRelativeTime(alert.createdAt)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
              <div className="mt-4 text-center">
                <button
                  onClick={() => navigate('/dashboard/notifications')}
                  className="text-sm text-primary-500 hover:text-primary-400 transition-colors"
                >
                  Tüm uyarıları görüntüle →
                </button>
              </div>
            </CardContent>
          </Card>
        </div>

        <div>
          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {quickActions.map(({ key, label, description, icon: Icon, onClick, variant }) => (
                <button
                  key={key}
                  onClick={onClick}
                  className={cn(
                    'w-full rounded-lg p-3 text-left transition-colors',
                    getQuickActionClasses(variant)
                  )}
                >
                  <div className="flex items-center space-x-3">
                    <Icon className={cn(
                      'h-5 w-5 flex-shrink-0',
                      variant === 'primary' 
                        ? 'text-white' 
                        : variant === 'tertiary'
                        ? 'text-primary-600 dark:text-primary-300'
                        : 'text-gray-700 dark:text-gray-300'
                    )} />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium">{label}</p>
                      {description && (
                        <p className={cn(
                          'text-xs mt-0.5',
                          variant === 'primary'
                            ? 'text-white/90'
                            : variant === 'tertiary'
                            ? 'text-primary-600/80 dark:text-primary-300/80'
                            : 'text-gray-600 dark:text-gray-400'
                        )}>
                          {description}
                        </p>
                      )}
                    </div>
                  </div>
                </button>
              ))}
            </CardContent>
          </Card>

          {isAdmin && (
            <Card className="mt-6">
              <CardHeader>
                <CardTitle>System Status</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {systemStatusRows.map((row) => (
                    <div key={row.label} className="flex items-center justify-between">
                      <span className="text-sm text-surface-muted">{row.label}</span>
                      <span className="flex items-center space-x-2">
                        <div className={cn('h-2 w-2 rounded-full', row.active ? 'bg-success' : 'bg-danger')} />
                        <span className={cn('text-sm', row.active ? 'text-success' : 'text-danger')}>
                          {row.active ? 'Aktif' : 'Pasif'}
                        </span>
                      </span>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Tool Usage Chart */}
        <Card>
          <CardHeader>
            <CardTitle>Kullanılan Araçların Sıklığı</CardTitle>
          </CardHeader>
          <CardContent>
            {chartsLoading ? (
              <div className="h-64 flex items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-primary-500" />
              </div>
            ) : toolUsageData && toolUsageData.tools ? (
              toolUsageData.tools.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart
                    data={toolUsageData.tools}
                    layout="vertical"
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                    <XAxis type="number" stroke="#9ca3af" />
                    <YAxis dataKey="name" type="category" stroke="#9ca3af" width={100} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#1f2937',
                        border: '1px solid #374151',
                        borderRadius: '8px',
                        color: '#fff',
                      }}
                    />
                    <Bar dataKey="count" fill="#3b82f6" radius={[0, 8, 8, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-64 flex items-center justify-center text-surface-muted">
                  <p>Henüz araç kullanım verisi yok</p>
                </div>
              )
            ) : (
              <div className="h-64 flex items-center justify-center text-surface-muted">
                <p>Veri yükleniyor...</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* User Activity Chart */}
        <Card>
          <CardHeader>
            <CardTitle>Kullanıcı Aktivite Durumu</CardTitle>
          </CardHeader>
          <CardContent>
            {chartsLoading ? (
              <div className="h-64 flex items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-primary-500" />
              </div>
            ) : userActivityData ? (
              <div className="space-y-4">
                {(userActivityData.active > 0 || userActivityData.inactive > 0) ? (
                  <>
                    <ResponsiveContainer width="100%" height={200}>
                      <PieChart>
                        <Pie
                          data={[
                            { name: 'Aktif', value: userActivityData.active || 0, color: '#22c55e' },
                            { name: 'Pasif', value: userActivityData.inactive || 0, color: '#6b7280' },
                          ]}
                          cx="50%"
                          cy="50%"
                          labelLine={false}
                          label={({ name, percent }) => {
                            const total = (userActivityData.active || 0) + (userActivityData.inactive || 0);
                            if (total === 0) return `${name}: 0%`;
                            return `${name}: ${(percent * 100).toFixed(1)}%`;
                          }}
                          outerRadius={80}
                          fill="#8884d8"
                          dataKey="value"
                        >
                          <Cell fill="#22c55e" />
                          <Cell fill="#6b7280" />
                        </Pie>
                        <Tooltip
                          contentStyle={{
                            backgroundColor: '#1f2937',
                            border: '1px solid #374151',
                            borderRadius: '8px',
                            color: '#fff',
                          }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                    <div className="flex items-center justify-center space-x-6 text-sm">
                      <div className="flex items-center space-x-2">
                        <div className="h-3 w-3 rounded-full bg-success"></div>
                        <span className="text-surface-muted">
                          Aktif: {userActivityData.active || 0} ({userActivityData.activePercentage?.toFixed(1) || 0}%)
                        </span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="h-3 w-3 rounded-full bg-surface-muted"></div>
                        <span className="text-surface-muted">
                          Pasif: {userActivityData.inactive || 0} ({userActivityData.inactivePercentage?.toFixed(1) || 0}%)
                        </span>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="h-64 flex items-center justify-center text-surface-muted">
                    <p>Henüz kullanıcı verisi yok</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="h-64 flex items-center justify-center text-surface-muted">
                <p>Veri yükleniyor...</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Scan Status Chart */}
        <Card>
          <CardHeader>
            <CardTitle>Tarama Durumları</CardTitle>
          </CardHeader>
          <CardContent>
            {chartsLoading ? (
              <div className="h-64 flex items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-primary-500" />
              </div>
            ) : scanStatusData && scanStatusData.statuses ? (
              scanStatusData.statuses.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={scanStatusData.statuses}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                    <XAxis dataKey="status" stroke="#9ca3af" />
                    <YAxis stroke="#9ca3af" />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#1f2937',
                        border: '1px solid #374151',
                        borderRadius: '8px',
                        color: '#fff',
                      }}
                    />
                    <Bar dataKey="count" fill="#3b82f6" radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-64 flex items-center justify-center text-surface-muted">
                  <p>Henüz tarama verisi yok</p>
                </div>
              )
            ) : (
              <div className="h-64 flex items-center justify-center text-surface-muted">
                <p>Veri yükleniyor...</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;
