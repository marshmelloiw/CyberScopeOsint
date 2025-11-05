import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import RiskBadge from '../../components/common/RiskBadge';
import { ArrowLeft, Loader2, AlertCircle, CheckCircle, XCircle, Globe, Mail, MapPin, Users, Trash2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const ScanDetail = () => {
  const { scanId } = useParams();
  const navigate = useNavigate();
  const [scan, setScan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    const fetchScanDetails = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Fetch scan status and results
        const response = await api.get(`/scans/status/${scanId}`);
        const scanData = response.data;
        
        setScan(scanData);
      } catch (err) {
        console.error('Error fetching scan details:', err);
        setError(err.response?.data?.error || 'Scan detayları yüklenemedi');
      } finally {
        setLoading(false);
      }
    };

    if (scanId) {
      fetchScanDetails();
      
      // Poll for updates if scan is still running
      const interval = setInterval(() => {
        if (scan?.status === 'RUNNING') {
          fetchScanDetails();
        }
      }, 3000);
      
      return () => clearInterval(interval);
    }
  }, [scanId]);

  const getTypeIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'domain': return <Globe className="h-5 w-5" />;
      case 'email': return <Mail className="h-5 w-5" />;
      case 'ip': return <MapPin className="h-5 w-5" />;
      case 'social': return <Users className="h-5 w-5" />;
      default: return <Globe className="h-5 w-5" />;
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED': return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'RUNNING': return <Loader2 className="h-5 w-5 animate-spin text-blue-500" />;
      case 'FAILED': return <XCircle className="h-5 w-5 text-red-500" />;
      default: return <AlertCircle className="h-5 w-5 text-yellow-500" />;
    }
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString('tr-TR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        <span className="ml-3 text-surface-muted">Scan detayları yükleniyor...</span>
      </div>
    );
  }

  if (error || !scan) {
    return (
      <div className="space-y-6">
        <Button variant="outline" onClick={() => navigate('/dashboard/scans')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Geri Dön
        </Button>
        <Card>
          <CardContent className="p-12 text-center">
            <AlertCircle className="h-16 w-16 mx-auto mb-4 text-danger" />
            <h3 className="text-xl font-semibold text-white mb-2">Scan Bulunamadı</h3>
            <p className="text-surface-muted mb-4">{error || 'Scan detayları yüklenemedi'}</p>
            <Button onClick={() => navigate('/dashboard/scans')}>Scan Listesine Dön</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const result = scan.result || {};
  const results = result.results || result.data || {};
  const geminiReports = result.gemini_reports || {};
  const logs = scan.logs || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button variant="outline" onClick={() => navigate('/dashboard/scans')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Geri Dön
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-white">{result.name || 'Scan Detayları'}</h1>
            <p className="text-surface-muted">Scan ID: {scan.scanId}</p>
          </div>
        </div>
        <Button 
          variant="outline" 
          className="text-danger hover:text-danger hover:border-danger"
          onClick={async () => {
            if (window.confirm(`"${result.name || 'Bu scan'}" scan'ini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
              try {
                console.log('Deleting scan:', scan.scanId);
                await api.delete(`/scans/${scan.scanId}`);
                console.log('Scan deleted successfully');
                alert('Scan başarıyla silindi');
                navigate('/dashboard/scans');
              } catch (err) {
                console.error('Error deleting scan:', err);
                const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                alert('Scan silinirken hata oluştu: ' + errorMsg);
              }
            }
          }}
        >
          <Trash2 className="h-4 w-4 mr-2" />
          Scan'i Sil
        </Button>
      </div>

      {/* Scan Info */}
      <Card>
        <CardHeader>
          <CardTitle>Scan Bilgileri</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <p className="text-sm text-surface-muted mb-1">Durum</p>
              <div className="flex items-center space-x-2">
                {getStatusIcon(scan.status)}
                <span className="text-white capitalize">{scan.status?.toLowerCase()}</span>
              </div>
            </div>
            <div>
              <p className="text-sm text-surface-muted mb-1">Tip</p>
              <div className="flex items-center space-x-2">
                {getTypeIcon(result.type)}
                <span className="text-white capitalize">{result.type}</span>
              </div>
            </div>
            <div>
              <p className="text-sm text-surface-muted mb-1">Risk Seviyesi</p>
              <RiskBadge score={scan.status === 'COMPLETED' ? 5 : 0} />
            </div>
            <div>
              <p className="text-sm text-surface-muted mb-1">Başlangıç</p>
              <p className="text-white">{formatTimestamp(scan.logs?.[0]?.timestamp)}</p>
            </div>
          </div>
          
          {result.targets && result.targets.length > 0 && (
            <div className="mt-4">
              <p className="text-sm text-surface-muted mb-2">Hedefler</p>
              <div className="flex flex-wrap gap-2">
                {result.targets.map((target, idx) => (
                  <span key={idx} className="px-3 py-1 bg-surface-panel rounded-lg text-white">
                    {target}
                  </span>
                ))}
              </div>
            </div>
          )}

          {result.providers && result.providers.length > 0 && (
            <div className="mt-4">
              <p className="text-sm text-surface-muted mb-2">Provider'lar</p>
              <div className="flex flex-wrap gap-2">
                {result.providers.map((provider, idx) => (
                  <span key={idx} className="px-3 py-1 bg-primary-600 rounded-lg text-white">
                    {provider}
                  </span>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Tabs */}
      <div className="border-b border-surface-border">
        <nav className="flex space-x-8">
          {['overview', 'results', 'gemini', 'logs'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
                activeTab === tab
                  ? 'border-primary-500 text-primary-400'
                  : 'border-transparent text-surface-muted hover:text-white hover:border-surface-border'
              )}
            >
              {tab === 'overview' && 'Genel Bakış'}
              {tab === 'results' && 'Sonuçlar'}
              {tab === 'gemini' && 'AI Analiz'}
              {tab === 'logs' && 'Loglar'}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="mt-6">
        {activeTab === 'overview' && (
          <Card>
            <CardHeader>
              <CardTitle>Genel Bakış</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <p className="text-sm text-surface-muted mb-2">Scan Adı</p>
                  <p className="text-white">{result.name || 'İsimsiz Scan'}</p>
                </div>
                {scan.completedAt && (
                  <div>
                    <p className="text-sm text-surface-muted mb-2">Tamamlanma Zamanı</p>
                    <p className="text-white">{formatTimestamp(scan.completedAt)}</p>
                  </div>
                )}
                {scan.errorMessage && (
                  <div>
                    <p className="text-sm text-surface-muted mb-2">Hata Mesajı</p>
                    <p className="text-red-400">{scan.errorMessage}</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'results' && (
          <Card>
            <CardHeader>
              <CardTitle>Scan Sonuçları</CardTitle>
            </CardHeader>
            <CardContent>
              {Object.keys(results).length === 0 ? (
                <p className="text-surface-muted">Henüz sonuç yok</p>
              ) : (
                <div className="space-y-6">
                  {Object.entries(results).map(([target, targetResults]) => (
                    <div key={target} className="border border-surface-border rounded-lg p-4">
                      <h3 className="text-lg font-semibold text-white mb-4">{target}</h3>
                      {targetResults && typeof targetResults === 'object' && (
                        <div className="space-y-4">
                          {Object.entries(targetResults).map(([provider, providerData]) => (
                            <div key={provider} className="bg-surface-panel rounded-lg p-4">
                              <h4 className="font-medium text-white mb-2">{provider}</h4>
                              {providerData && typeof providerData === 'object' && (
                                <pre className="text-xs text-surface-muted overflow-x-auto bg-surface-border p-3 rounded">
                                  {JSON.stringify(providerData, null, 2)}
                                </pre>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {activeTab === 'gemini' && (
          <Card>
            <CardHeader>
              <CardTitle>AI Analiz Raporları (Gemini)</CardTitle>
            </CardHeader>
            <CardContent>
              {Object.keys(geminiReports).length === 0 ? (
                <p className="text-surface-muted">
                  {scan.status === 'RUNNING' 
                    ? 'AI analiz raporları hala oluşturuluyor...'
                    : 'Henüz AI analiz raporu yok'}
                </p>
              ) : (
                <div className="space-y-6">
                  {Object.entries(geminiReports).map(([key, report]) => (
                    <div key={key} className="border border-surface-border rounded-lg p-4">
                      <h3 className="text-lg font-semibold text-white mb-4">{key}</h3>
                      {report && typeof report === 'object' && (
                        <div className="prose prose-invert max-w-none">
                          {report.summary && (
                            <div className="mb-4">
                              <h4 className="text-white font-medium mb-2">Özet</h4>
                              <p className="text-surface-muted whitespace-pre-wrap">{report.summary}</p>
                            </div>
                          )}
                          {report.analysis && (
                            <div className="mb-4">
                              <h4 className="text-white font-medium mb-2">Analiz</h4>
                              <p className="text-surface-muted whitespace-pre-wrap">{report.analysis}</p>
                            </div>
                          )}
                          {report.recommendations && (
                            <div>
                              <h4 className="text-white font-medium mb-2">Öneriler</h4>
                              <p className="text-surface-muted whitespace-pre-wrap">{report.recommendations}</p>
                            </div>
                          )}
                          {!report.summary && !report.analysis && !report.recommendations && (
                            <pre className="text-xs text-surface-muted overflow-x-auto bg-surface-border p-3 rounded">
                              {JSON.stringify(report, null, 2)}
                            </pre>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {activeTab === 'logs' && (
          <Card>
            <CardHeader>
              <CardTitle>Scan Logları</CardTitle>
            </CardHeader>
            <CardContent>
              {logs.length === 0 ? (
                <p className="text-surface-muted">Henüz log yok</p>
              ) : (
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {logs.map((log, idx) => (
                    <div key={idx} className="flex items-start space-x-3 text-sm">
                      <span className="text-surface-muted min-w-[120px]">
                        {formatTimestamp(log.timestamp)}
                      </span>
                      <span className="text-white flex-1">{log.message}</span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default ScanDetail;

