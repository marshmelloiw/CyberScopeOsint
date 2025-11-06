import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import RiskBadge from '../../components/common/RiskBadge';
import { Search, FileText, Eye, Trash2, Globe, Mail, MapPin, Users, Loader2, RefreshCw, AlertCircle, Download, FileCode } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const Reports = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Fetch scan reports from API
  const fetchReports = useCallback(async (showRefreshing = false) => {
    try {
      if (showRefreshing) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      
      // Get scans with Gemini reports from dedicated endpoint
      const response = await api.get('/scans/reports');
      const scansData = response.data.reports || [];
      
      const completedScans = scansData;
      
      const transformedReports = completedScans.map((scan) => {
        const targets = scan.targets || [];
        const providers = scan.providers || [];
        const timestamp = scan.timestamp || scan.createdAt || 0;
        const completedAt = scan.completedAt || scan.createdAt;
        const geminiReports = scan.geminiReports || {};
        
        // Generate title if not provided
        let title = scan.name;
        if (!title && targets.length > 0) {
          const typeName = scan.type ? scan.type.charAt(0).toUpperCase() + scan.type.slice(1) : 'Scan';
          title = `${typeName} AI Report - ${targets.join(', ')}`;
        }
        
        // Count Gemini reports
        const geminiReportCount = Object.keys(geminiReports).length;
        
        return {
          id: scan.scanId,
          scanId: scan.scanId,
          title: title || 'Unnamed AI Report',
          type: scan.type || 'unknown',
      status: 'completed',
          createdAt: timestamp ? new Date(timestamp).toISOString() : completedAt,
          updatedAt: completedAt || new Date().toISOString(),
          targets: Array.isArray(targets) ? targets : [],
          providers: Array.isArray(providers) ? providers : [],
          findings: scan.findings || 0,
          riskScore: scan.findings > 20 ? 8 : scan.findings > 10 ? 5 : scan.findings > 5 ? 3 : 1,
          geminiReportCount,
        };
      });
      
      setReports(transformedReports);
    } catch (err) {
      console.error('Error fetching reports:', err);
      setError(err.response?.data?.error || 'Rapor listesi yüklenemedi');
      setReports([]);
    } finally {
      if (showRefreshing) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    fetchReports();
    
    // Refresh every 10 seconds
    const interval = setInterval(() => fetchReports(false), 10000);
    return () => clearInterval(interval);
  }, [fetchReports]);

  const getTypeIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'domain': return <Globe className="h-5 w-5" />;
      case 'email': return <Mail className="h-5 w-5" />;
      case 'ip': return <MapPin className="h-5 w-5" />;
      case 'social': return <Users className="h-5 w-5" />;
      default: return <FileText className="h-5 w-5" />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed': return 'bg-success text-white';
      case 'running': return 'bg-info text-white';
      case 'failed': return 'bg-danger text-white';
      default: return 'bg-surface-muted text-white';
    }
  };

  const filteredReports = reports.filter(report => {
    const matchesSearch = report.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      report.targets.some(target => target.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'all' || report.status === statusFilter;
    const matchesType = typeFilter === 'all' || report.type === typeFilter;

    return matchesSearch && matchesStatus && matchesType;
  });

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">AI Reports</h1>
          <p className="text-surface-muted">View and manage your Gemini AI analysis reports</p>
        </div>
        <Button 
          variant="outline" 
          onClick={() => fetchReports(true)}
          disabled={refreshing || loading}
          className="flex items-center space-x-2"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </Button>
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
                  placeholder="Search reports by title or target..."
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
              <option value="social">Social Media</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Reports list */}
      <Card>
        <CardHeader>
          <CardTitle>Gemini AI Reports ({reports.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
              <span className="ml-3 text-surface-muted">Raporlar yükleniyor...</span>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <AlertCircle className="h-16 w-16 mx-auto mb-4 text-danger" />
                <p className="text-danger mb-2">{error}</p>
                <Button onClick={() => fetchReports(false)} variant="outline">
                  Tekrar Dene
                </Button>
              </div>
            </div>
          ) : filteredReports.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <p className="text-surface-muted mb-4">
                  {reports.length === 0 
                    ? 'Henüz Gemini AI raporu yok. Tamamlanan scan\'lerin AI analiz raporları burada görünecek.'
                    : 'Arama kriterlerinize uygun rapor bulunamadı.'}
                </p>
                <Link to="/dashboard/scans/new">
                  <Button>
                    Yeni Scan Oluştur
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
          <div className="space-y-4">
            {filteredReports.map((report) => (
              <div
                key={report.id}
                className="p-4 border border-surface-border rounded-lg hover:bg-surface-panel/50 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      {getTypeIcon(report.type)}
                      <h3 className="text-lg font-semibold text-white">{report.title}</h3>
                      <span className={cn(
                          'px-2 py-1 rounded-full text-xs font-medium capitalize',
                        getStatusColor(report.status)
                      )}>
                        {report.status}
                      </span>
                    </div>

                      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm mb-3">
                      <div>
                          <span className="text-surface-muted">Targets:</span>
                          <span className="text-white ml-2">
                            {report.targets.length > 0 ? report.targets.join(', ') : 'N/A'}
                          </span>
                      </div>
                      <div>
                          <span className="text-surface-muted">Providers:</span>
                          <span className="text-white ml-2">
                            {report.providers.length > 0 ? report.providers.join(', ') : 'N/A'}
                          </span>
                      </div>
                      <div>
                          <span className="text-surface-muted">AI Reports:</span>
                          <span className="text-white ml-2">{report.geminiReportCount || 0}</span>
                      </div>
                      <div>
                          <span className="text-surface-muted">Risk:</span>
                          <span className="ml-2">
                            <RiskBadge risk={Number(report.riskScore ?? 0)} />
                          </span>
                      </div>
                    </div>

                      <div className="flex items-center space-x-4 text-sm text-surface-muted">
                      <div className="flex items-center space-x-1">
                          <span>Created: {new Date(report.createdAt).toLocaleDateString('tr-TR')}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                          <span className="capitalize">{report.type}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col space-y-2 ml-4">
                      <Link to={`/dashboard/reports/${report.scanId}`}>
                        <Button variant="ghost" size="sm" className="w-full">
                      <Eye className="h-4 w-4 mr-2" />
                      View
                    </Button>
                      </Link>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        className="text-danger hover:text-danger w-full"
                        onClick={async (e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          if (window.confirm(`"${report.title}" raporunu silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
                            try {
                              console.log('Deleting report/scan:', report.scanId);
                              await api.delete(`/scans/${report.scanId}`);
                              console.log('Report/scan deleted successfully');
                              await fetchReports(false);
                              alert('Rapor başarıyla silindi');
                            } catch (err) {
                              console.error('Error deleting report:', err);
                              const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                              alert('Rapor silinirken hata oluştu: ' + errorMsg);
                            }
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete
                    </Button>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        className="w-full"
                        onClick={async (e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          try {
                            const response = await api.get(`/scans/${report.scanId}/report/pdf`, {
                              responseType: 'blob'
                            });
                            
                            // Create blob URL and trigger download
                            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
                            const link = document.createElement('a');
                            link.href = url;
                            link.setAttribute('download', `report_${report.scanId}.pdf`);
                            document.body.appendChild(link);
                            link.click();
                            link.remove();
                            window.URL.revokeObjectURL(url);
                          } catch (err) {
                            console.error('Error downloading PDF:', err);
                            const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                            alert('PDF indirilirken hata oluştu: ' + errorMsg);
                          }
                        }}
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Download PDF
                    </Button>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        className="w-full"
                        onClick={async (e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          try {
                            const response = await api.get(`/scans/${report.scanId}/report/html`, {
                              responseType: 'blob'
                            });
                            
                            // Create blob URL and trigger download
                            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/html' }));
                            const link = document.createElement('a');
                            link.href = url;
                            link.setAttribute('download', `report_${report.scanId}.html`);
                            document.body.appendChild(link);
                            link.click();
                            link.remove();
                            window.URL.revokeObjectURL(url);
                          } catch (err) {
                            console.error('Error downloading HTML:', err);
                            const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                            alert('HTML indirilirken hata oluştu: ' + errorMsg);
                          }
                        }}
                      >
                        <FileCode className="h-4 w-4 mr-2" />
                        Download HTML
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Reports;
