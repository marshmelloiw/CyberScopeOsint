import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { ArrowLeft, Loader2, AlertCircle, Trash2 } from 'lucide-react';
import api from '../../lib/axios';
import MarkdownRenderer from '../../components/common/MarkdownRenderer';
import { resolveReportMarkdown } from '../../lib/reportMarkdown';

const GeminiReportDetail = () => {
  const { scanId } = useParams();
  const navigate = useNavigate();
  const [scan, setScan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchGeminiReports = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Fetch scan status to get Gemini reports
        const response = await api.get(`/scans/status/${scanId}`);
        const scanData = response.data;
        
        setScan(scanData);
      } catch (err) {
        console.error('Error fetching Gemini reports:', err);
        setError(err.response?.data?.error || 'Gemini raporları yüklenemedi');
      } finally {
        setLoading(false);
      }
    };

    if (scanId) {
      fetchGeminiReports();
    }
  }, [scanId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        <span className="ml-3 text-surface-muted">Gemini raporları yükleniyor...</span>
      </div>
    );
  }

  if (error || !scan) {
    return (
      <div className="space-y-6">
        <Button variant="outline" onClick={() => navigate('/dashboard/reports')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Geri Dön
        </Button>
        <Card>
          <CardContent className="p-12 text-center">
            <AlertCircle className="h-16 w-16 mx-auto mb-4 text-danger" />
            <h3 className="text-xl font-semibold text-white mb-2">Gemini Raporu Bulunamadı</h3>
            <p className="text-surface-muted mb-4">{error || 'Gemini raporları yüklenemedi'}</p>
            <Button onClick={() => navigate('/dashboard/reports')}>Rapor Listesine Dön</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const result = scan.result || {};
  const geminiReports = result.gemini_reports || {};
  const scanName = result.name || 'Gemini AI Raporu';
  const targets = result.targets || [];
  const providers = result.providers || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button variant="outline" onClick={() => navigate('/dashboard/reports')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Geri Dön
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-white">{scanName}</h1>
            <p className="text-surface-muted">Gemini AI Analiz Raporları</p>
          </div>
        </div>
        <Button 
          variant="outline" 
          className="text-danger hover:text-danger hover:border-danger"
          onClick={async () => {
            if (window.confirm(`"${scanName}" raporunu silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
              try {
                console.log('Deleting scan:', scan.scanId);
                await api.delete(`/scans/${scan.scanId}`);
                console.log('Scan deleted successfully');
                alert('Rapor başarıyla silindi');
                navigate('/dashboard/reports');
              } catch (err) {
                console.error('Error deleting scan:', err);
                const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
                alert('Rapor silinirken hata oluştu: ' + errorMsg);
              }
            }
          }}
        >
          <Trash2 className="h-4 w-4 mr-2" />
          Raporu Sil
        </Button>
      </div>

      {/* Scan Info */}
      <Card>
        <CardHeader>
          <CardTitle>Rapor Bilgileri</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {targets.length > 0 && (
              <div>
                <p className="text-sm text-surface-muted mb-2">Hedefler</p>
                <div className="flex flex-wrap gap-2">
                  {targets.map((target, idx) => (
                    <span key={idx} className="px-3 py-1 bg-surface-panel rounded-lg text-white">
                      {target}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {providers.length > 0 && (
              <div>
                <p className="text-sm text-surface-muted mb-2">Provider'lar</p>
                <div className="flex flex-wrap gap-2">
                  {providers.map((provider, idx) => (
                    <span key={idx} className="px-3 py-1 bg-primary-600 rounded-lg text-white">
                      {provider}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Gemini Reports */}
      <Card>
        <CardHeader>
          <CardTitle>AI Analiz Raporları (Gemini)</CardTitle>
        </CardHeader>
        <CardContent>
          {Object.keys(geminiReports).length === 0 ? (
            <div className="text-center py-12">
              <p className="text-surface-muted mb-2">
                Henüz AI analiz raporu yok
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              {Object.entries(geminiReports).map(([key, report]) => {
                const [provider, target] = key.split('_');
                const reportData = typeof report === 'object' ? report : {};
                
                return (
                  <div key={key} className="border border-surface-border rounded-lg p-6 bg-surface-panel/50">
                    <div className="mb-4 pb-4 border-b border-surface-border">
                      <h3 className="text-lg font-semibold text-white mb-2">
                        {provider} - {target}
                      </h3>
                      {reportData.status && (
                        <span className="text-sm text-surface-muted">
                          {reportData.status === 'completed' ? '✓ Tamamlandı' : '⏳ Oluşturuluyor...'}
                        </span>
                      )}
                    </div>
                    
                    {/* Display report content */}
                    {
                      (() => {
                        const markdownContent = resolveReportMarkdown(
                          typeof report === 'string' ? report : reportData
                        );

                        if (!markdownContent) {
                          return (
                            <pre className="text-xs text-surface-muted overflow-x-auto bg-surface-border p-4 rounded">
                              {JSON.stringify(reportData, null, 2)}
                            </pre>
                          );
                        }

                        return (
                          <div className="space-y-4">
                            <MarkdownRenderer content={markdownContent} />
                          </div>
                        );
                      })()
                    }
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default GeminiReportDetail;

