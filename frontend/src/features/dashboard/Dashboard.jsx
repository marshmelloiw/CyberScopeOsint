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
} from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const Dashboard = () => {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchSummary();
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
              <button
                onClick={() => navigate('/dashboard/scans/new')}
                className="w-full rounded-lg bg-primary-600 p-3 text-left text-white hover:bg-primary-700 transition-colors"
              >
                <div className="flex items-center space-x-2">
                  <Search className="h-5 w-5" />
                  <span>Yeni Tarama</span>
                </div>
              </button>
              <button
                onClick={() => navigate('/dashboard/reports')}
                className="w-full rounded-lg bg-surface-panel p-3 text-left text-white hover:bg-surface-border transition-colors border border-surface-border"
              >
                <div className="flex items-center space-x-2">
                  <FileSearch className="h-5 w-5" />
                  <span>Rapor Oluştur</span>
                </div>
              </button>
            </CardContent>
          </Card>

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
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Security Trends</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-64 flex items-center justify-center text-surface-muted">
            <p>Grafikler yakında gelecektir.</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Dashboard;
