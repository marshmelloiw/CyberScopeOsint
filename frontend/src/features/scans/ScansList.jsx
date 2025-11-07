import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import RiskBadge from '../../components/common/RiskBadge';
import { Search, Plus, Filter, Eye, Play, Pause, Trash2, Loader2, RefreshCw } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const PROVIDER_LABELS = {
  ZAP: 'OWASP ZAP',
};

const TYPE_LABELS = {
  url: 'Web Application',
};

const ScansList = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [scans, setScans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const getProviderLabel = (provider) => {
    if (!provider) return provider;
    const normalized = provider.toUpperCase();
    return PROVIDER_LABELS[normalized] || provider;
  };

  const getTypeLabel = (type) => {
    if (!type) return 'Unknown';
    const normalized = type.toLowerCase();
    if (TYPE_LABELS[normalized]) {
      return TYPE_LABELS[normalized];
    }
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  };

  const formatProviderList = (list = []) => list.map(getProviderLabel).join(', ');

  // Fetch scans from API
  const fetchScans = useCallback(async (showRefreshing = false) => {
    try {
      if (showRefreshing) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      const response = await api.get(endpoints.scans.list);
      const scansData = response.data.scans || [];
      
      // Transform API data to match component format
      const transformedScans = scansData.map((scan) => {
        const targets = scan.targets || [];
        const providers = scan.providers || [];
        const timestamp = scan.timestamp || 0;
        const completedAt = scan.completedAt;
        
        // Generate name if not provided
        let name = scan.name;
        if (!name && targets.length > 0) {
          const typeName = scan.type ? getTypeLabel(scan.type) : 'Scan';
          name = `${typeName} - ${targets.join(', ')}`;
        }
        
        return {
          id: scan.scanId,
          scanId: scan.scanId,
          name: name || 'Unnamed Scan',
          type: scan.type || 'unknown',
          targets: Array.isArray(targets) ? targets : [],
          status: (scan.status || 'unknown').toLowerCase(),
          startedAt: timestamp ? new Date(timestamp).toISOString() : null,
          finishedAt: completedAt ? completedAt : null,
          findings: scan.findings || 0,
          providers: Array.isArray(providers) ? providers : [],
        };
      });
      
      setScans(transformedScans);
    } catch (err) {
      console.error('Error fetching scans:', err);
      setError(err.response?.data?.error || 'Scan listesi yüklenemedi');
      setScans([]);
    } finally {
      if (showRefreshing) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    fetchScans();
    
    // Refresh every 5 seconds to get updated scan statuses
    const interval = setInterval(() => fetchScans(false), 5000);
    return () => clearInterval(interval);
  }, [fetchScans]);

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed': return 'bg-success text-white';
      case 'running': return 'bg-info text-white';
      case 'queued': return 'bg-warning text-white';
      case 'failed': return 'bg-danger text-white';
      default: return 'bg-surface-muted text-white';
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'domain': return '🌐';
      case 'email': return '📧';
      case 'ip': return '🔌';
      case 'url': return '🛡️';
      case 'social': return '👥';
      default: return '🔍';
    }
  };

  const filteredScans = scans.filter(scan => {
    const matchesSearch = scan.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         scan.targets.some(target => target.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'all' || scan.status === statusFilter;
    const matchesType = typeFilter === 'all' || scan.type === typeFilter;
    
    return matchesSearch && matchesStatus && matchesType;
  });

  // Calculate risk score based on findings (simple heuristic)
  const getRiskScore = (scan) => {
    if (scan.status === 'failed') return 10;
    if (scan.status === 'running' || scan.status === 'queued') return 0;
    const findings = scan.findings || 0;
    if (findings > 20) return 8;
    if (findings > 10) return 5;
    if (findings > 5) return 3;
    return 1;
  };

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Scan History</h1>
          <p className="text-surface-muted">View and manage your security scan history</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button 
            variant="outline" 
            onClick={() => fetchScans(true)}
            disabled={refreshing || loading}
            className="flex items-center space-x-2"
            title="Refresh scan list"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </Button>
        <Link to="/dashboard/scans/new">
          <Button className="flex items-center space-x-2">
            <Plus className="h-4 w-4" />
            <span>New Scan</span>
          </Button>
        </Link>
        </div>
      </div>

      {/* Filters and search */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-muted" />
                <Input
                  placeholder="Search scans by name or target..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            {/* Status filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="all">All Status</option>
              <option value="completed">Completed</option>
              <option value="running">Running</option>
              <option value="queued">Queued</option>
              <option value="failed">Failed</option>
            </select>

            {/* Type filter */}
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="all">All Types</option>
              <option value="domain">Domain</option>
              <option value="email">Email</option>
              <option value="ip">IP Address</option>
              <option value="url">Web Application</option>
              <option value="social">Social Media</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Scans table */}
      <Card>
        <CardHeader>
          <CardTitle>Scan History ({scans.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
              <span className="ml-3 text-surface-muted">Scan listesi yükleniyor...</span>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <p className="text-danger mb-2">{error}</p>
                <Button onClick={() => window.location.reload()} variant="outline">
                  Tekrar Dene
                </Button>
              </div>
            </div>
          ) : filteredScans.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <p className="text-surface-muted mb-4">
                  {scans.length === 0 
                    ? 'Henüz scan yapılmamış. İlk scan\'inizi oluşturmak için "New Scan" butonuna tıklayın.'
                    : 'Arama kriterlerinize uygun scan bulunamadı.'}
                </p>
                {scans.length === 0 && (
                  <Link to="/dashboard/scans/new">
                    <Button>
                      <Plus className="h-4 w-4 mr-2" />
                      Yeni Scan Oluştur
                    </Button>
                  </Link>
                )}
              </div>
            </div>
          ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-surface-border">
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Scan</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Type</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Status</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Risk</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Findings</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Started</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredScans.map((scan) => (
                    <tr key={scan.scanId || scan.id} className="border-b border-surface-border/50 hover:bg-surface-panel/50">
                    <td className="p-3">
                      <div>
                        <p className="font-medium text-white">{scan.name}</p>
                        <p className="text-sm text-surface-muted">
                            {scan.targets.length > 0 
                              ? `Targets: ${scan.targets.join(', ')}`
                              : 'No targets'}
                          </p>
                          {scan.providers.length > 0 && (
                            <p className="text-xs text-surface-muted mt-1">
                              Providers: {formatProviderList(scan.providers)}
                            </p>
                          )}
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center space-x-2">
                        <span className="text-lg">{getTypeIcon(scan.type)}</span>
                        <span className="text-white">{getTypeLabel(scan.type)}</span>
                      </div>
                    </td>
                    <td className="p-3">
                      <span className={cn(
                          'px-2 py-1 rounded-full text-xs font-medium capitalize',
                        getStatusColor(scan.status)
                      )}>
                        {scan.status}
                      </span>
                    </td>
                    <td className="p-3">
                        <RiskBadge risk={getRiskScore(scan)} />
                    </td>
                    <td className="p-3">
                      <span className="text-white">{scan.findings}</span>
                    </td>
                    <td className="p-3">
                      <span className="text-surface-muted">
                          {scan.startedAt 
                            ? new Date(scan.startedAt).toLocaleString('tr-TR', {
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              })
                            : '-'}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center space-x-2">
                          <Link to={`/dashboard/scans/${scan.scanId || scan.id}`}>
                            <Button variant="ghost" size="sm" title="View Details">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </Link>
                        {scan.status === 'running' && (
                            <Button variant="ghost" size="sm" title="Pause">
                            <Pause className="h-4 w-4" />
                          </Button>
                        )}
                        {scan.status === 'queued' && (
                            <Button variant="ghost" size="sm" title="Start">
                            <Play className="h-4 w-4" />
                          </Button>
                        )}
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="text-danger hover:text-danger" 
                            title="Delete"
                            onClick={async (e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              if (window.confirm(`"${scan.name}" scan'ini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
                                try {
                                  const scanIdToDelete = scan.scanId || scan.id;
                                  console.log('Deleting scan:', scanIdToDelete);
                                  await api.delete(`/scans/${scanIdToDelete}`);
                                  console.log('Scan deleted successfully');
                                  // Refresh scan list
                                  await fetchScans(false);
                                  alert('Scan başarıyla silindi');
                                } catch (err) {
                                  console.error('Error deleting scan:', err);
                                  const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                                  alert('Scan silinirken hata oluştu: ' + errorMsg);
                                }
                              }
                            }}
                          >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default ScansList;
