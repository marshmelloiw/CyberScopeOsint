import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import axios from '../../lib/axios';
import { ArrowLeft, ArrowRight, Check, Globe, Mail, MapPin, Users, FileText, Terminal, Loader2 } from 'lucide-react';
import { cn } from '../../lib/utils';

const NewScan = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: '',
    targets: [],
    providers: [],
    schedule: 'immediate',
    notifications: true,
    tags: [],
  });
  const [isScanning, setIsScanning] = useState(false);
  const [scanId, setScanId] = useState(null);
  const [scanLogs, setScanLogs] = useState([]);
  const [scanStatus, setScanStatus] = useState(null);
  const [scanResult, setScanResult] = useState(null);
  const pollingIntervalRef = useRef(null);

  const scanTypes = [
    { id: 'domain', name: 'Domain Analysis', icon: Globe, description: 'Analyze domain security, DNS, and reputation' },
    { id: 'email', name: 'Email Breach Check', icon: Mail, description: 'Check email addresses against data breaches' },
    { id: 'ip', name: 'IP Address Analysis', icon: MapPin, description: 'Analyze IP addresses for threats and vulnerabilities' },
    { id: 'social', name: 'Social Media Monitor', icon: Users, description: 'Monitor social media accounts for threats' },
  ];

  const providers = {
    domain: ['VirusTotal', 'Shodan', 'Whois', 'AbuseIPDB', 'URLVoid'],
    email: ['HaveIBeenPwned', 'DeHashed', 'Intelligence X', 'LeakCheck'],
    ip: ['Shodan', 'VirusTotal', 'AbuseIPDB', 'IPQualityScore', 'IP2Location'],
    social: ['Twitter', 'LinkedIn', 'GitHub', 'Reddit', 'Telegram'],
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleTargetsChange = (value) => {
    const targets = value.split('\n').filter(target => target.trim());
    setFormData(prev => ({ ...prev, targets }));
  };

  const handleProviderToggle = (provider) => {
    setFormData(prev => ({
      ...prev,
      providers: prev.providers.includes(provider)
        ? prev.providers.filter(p => p !== provider)
        : [...prev.providers, provider]
    }));
  };

  const handleTagsChange = (value) => {
    const tags = value.split(',').map(tag => tag.trim()).filter(tag => tag);
    setFormData(prev => ({ ...prev, tags }));
  };

  const nextStep = () => {
    if (currentStep < 3) setCurrentStep(currentStep + 1);
  };

  const prevStep = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
  };

  // Poll scan status
  useEffect(() => {
    if (!scanId || !isScanning) {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
      return;
    }

    const pollStatus = async () => {
      try {
        const response = await axios.get(`/scans/status/${scanId}`, { timeout: 60000 });
        const status = response.data;
        
        setScanStatus(status.status);
        setScanLogs(status.logs || []);
        
        if (status.result) {
          setScanResult(status.result);
        }
        
        if (status.status === 'FAILED') {
          setIsScanning(false);
          if (pollingIntervalRef.current) {
            clearInterval(pollingIntervalRef.current);
            pollingIntervalRef.current = null;
          }
          return;
        }
        // Continue polling even after COMPLETED to get Gemini reports
        if (status.status === 'COMPLETED') {
          // Check if all reports are ready (not generating)
          const reports = status.result?.gemini_reports || {};
          const dataKeys = Object.keys(status.result?.data || {});
          
          // Check if there are any reports still generating
          const hasGeneratingReports = dataKeys.some(key => {
            const report = reports[key];
            return !report || report.status === 'generating';
          });
          
          // Only stop polling if all reports are ready (completed or failed)
          if (!hasGeneratingReports && dataKeys.length > 0) {
            setIsScanning(false);
            if (pollingIntervalRef.current) {
              clearInterval(pollingIntervalRef.current);
              pollingIntervalRef.current = null;
            }
          }
        }
      } catch (error) {
        console.error('Error polling scan status:', error);
      }
    };

    // Poll every 1 second
    pollingIntervalRef.current = setInterval(pollStatus, 1000);
    
    // Initial poll
    pollStatus();

    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, [scanId, isScanning]);

  const handleSubmit = async () => {
    try {
      setIsScanning(true);
      setScanLogs([]);
      setScanResult(null);
      
      // Start scan via API
      const scanRequest = {
        name: formData.name,
        type: formData.type,
        targets: formData.targets,
        providers: formData.providers,
      };
      
      console.log('Starting scan with request:', scanRequest);
      console.log('Axios instance:', axios);
      console.log('Full request URL will be: http://localhost:8080/api/scans/start');
      
      const response = await axios.post('/scans/start', scanRequest, { timeout: 60000 });
      const newScanId = response.data.scanId;
      
      setScanId(newScanId);
      setScanStatus('RUNNING');
      
      // Add initial logs
      setScanLogs([
        { timestamp: Date.now(), message: `Starting scan: ${formData.name}` },
        { timestamp: Date.now(), message: `Type: ${formData.type}` },
        { timestamp: Date.now(), message: `Targets: ${formData.targets.join(', ')}` },
        { timestamp: Date.now(), message: `Providers: ${formData.providers.join(', ')}` },
        { timestamp: Date.now(), message: 'Initializing scan execution...' },
      ]);
      
    } catch (error) {
      console.error('Error starting scan:', error);
      setIsScanning(false);
      alert('Failed to start scan: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleViewResults = () => {
    if (scanResult) {
      // Navigate to results page or show results in a modal
      navigate(`/scans/${scanId}`);
    }
  };

  const handleCloseTerminal = () => {
    setIsScanning(false);
    setScanId(null);
    setScanLogs([]);
    setScanStatus(null);
    setScanResult(null);
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
  };

  const isStepValid = () => {
    switch (currentStep) {
      case 1:
        return formData.name && formData.type && formData.targets.length > 0;
      case 2:
        return formData.providers.length > 0;
      case 3:
        return true;
      default:
        return false;
    }
  };

  const renderStep1 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Basic Information</h3>
        <p className="text-surface-muted">Provide basic details about your scan</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Scan Name</label>
          <Input
            placeholder="e.g., Domain Security Assessment - google.com"
            value={formData.name}
            onChange={(e) => handleInputChange('name', e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Description</label>
          <textarea
            placeholder="Optional description of the scan purpose..."
            value={formData.description}
            onChange={(e) => handleInputChange('description', e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            rows={3}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Scan Type</label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {scanTypes.map((type) => (
              <div
                key={type.id}
                className={cn(
                  'p-4 border rounded-lg cursor-pointer transition-all',
                  formData.type === type.id
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-border hover:border-primary-400'
                )}
                onClick={() => handleInputChange('type', type.id)}
              >
                <div className="flex items-center space-x-3">
                  <type.icon className="h-5 w-5 text-primary-400" />
                  <div>
                    <p className="font-medium text-white">{type.name}</p>
                    <p className="text-sm text-surface-muted">{type.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">
            Targets (one per line)
          </label>
          <textarea
            placeholder={formData.type === 'domain' ? 'google.com\nfacebook.com' : 
                       formData.type === 'email' ? 'test@example.com\nadmin@company.com' :
                       formData.type === 'ip' ? '8.8.8.8\n1.1.1.1' : '@username1\n@username2'}
            onChange={(e) => handleTargetsChange(e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            rows={4}
          />
          <p className="text-sm text-surface-muted mt-1">
            {formData.targets.length} target(s) specified
          </p>
        </div>
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Data Sources</h3>
        <p className="text-surface-muted">Select which intelligence providers to use</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Available Providers</label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {providers[formData.type]?.map((provider) => (
              <div
                key={provider}
                className={cn(
                  'p-3 border rounded-lg cursor-pointer transition-all',
                  formData.providers.includes(provider)
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-border hover:border-primary-400'
                )}
                onClick={() => handleProviderToggle(provider)}
              >
                <div className="flex items-center justify-between">
                  <span className="text-white">{provider}</span>
                  {formData.providers.includes(provider) && (
                    <Check className="h-5 w-5 text-primary-500" />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="p-4 bg-surface-panel/50 rounded-lg">
          <p className="text-sm text-surface-muted">
            <strong>Selected:</strong> {formData.providers.length} provider(s)
          </p>
          <p className="text-sm text-surface-muted mt-1">
            More providers = more comprehensive results, but longer scan time
          </p>
        </div>
      </div>
    </div>
  );

  const renderStep3 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Schedule & Settings</h3>
        <p className="text-surface-muted">Configure when and how to run the scan</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Schedule</label>
          <select
            value={formData.schedule}
            onChange={(e) => handleInputChange('schedule', e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          >
            <option value="immediate">Run immediately</option>
            <option value="scheduled">Schedule for later</option>
            <option value="recurring">Set up recurring scan</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Notifications</label>
          <div className="flex items-center space-x-3">
            <input
              type="checkbox"
              id="notifications"
              checked={formData.notifications}
              onChange={(e) => handleInputChange('notifications', e.target.checked)}
              className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="notifications" className="text-white">
              Send notifications when scan completes
            </label>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Tags (optional)</label>
          <Input
            placeholder="security, domain, assessment (comma separated)"
            value={formData.tags.join(', ')}
            onChange={(e) => handleTagsChange(e.target.value)}
          />
        </div>

        <div className="p-4 bg-surface-panel/50 rounded-lg">
          <h4 className="font-medium text-white mb-2">Scan Summary</h4>
          <div className="space-y-2 text-sm text-surface-muted">
            <p><strong>Name:</strong> {formData.name}</p>
            <p><strong>Type:</strong> {formData.type}</p>
            <p><strong>Targets:</strong> {formData.targets.length}</p>
            <p><strong>Providers:</strong> {formData.providers.length}</p>
            <p><strong>Schedule:</strong> {formData.schedule}</p>
          </div>
        </div>
      </div>
    </div>
  );

  const steps = [
    { number: 1, title: 'Basic Info', icon: FileText },
    { number: 2, title: 'Data Sources', icon: Globe },
    { number: 3, title: 'Settings', icon: Check },
  ];

  // Helper to check if reports are still generating
  const areReportsGenerating = () => {
    if (!scanResult || scanStatus !== 'COMPLETED') return false;
    const reports = scanResult.gemini_reports || {};
    const dataKeys = Object.keys(scanResult.data || {});
    return dataKeys.some(key => !reports[key] || reports[key]?.status === 'generating');
  };

  const stillGenerating = areReportsGenerating();
  const showLoading = isScanning && (scanStatus === 'RUNNING' || (scanStatus === 'COMPLETED' && stillGenerating));

  // Show scanning or results view
  if (isScanning || scanResult) {
    return (
      <div className="space-y-6">
        {/* Page header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button variant="ghost" onClick={handleCloseTerminal}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold text-white flex items-center gap-2">
                {showLoading ? (
                  <Loader2 className="h-8 w-8 text-primary-500 animate-spin" />
                ) : (
                  <Terminal className="h-8 w-8 text-primary-500" />
                )}
                {isScanning && scanStatus === 'RUNNING' ? 'Running Scan...' 
                  : stillGenerating ? 'Scan Completed - Generating Reports...'
                  : 'Scan Results'}
              </h1>
              <p className="text-surface-muted">
                {isScanning && scanStatus === 'RUNNING' 
                  ? 'Please wait while we analyze your targets' 
                  : stillGenerating
                  ? 'AI analysis reports are being generated...'
                  : 'Scan completed successfully'}
              </p>
            </div>
          </div>
          {scanStatus === 'COMPLETED' && !stillGenerating && (
            <Button onClick={handleViewResults} className="bg-success hover:bg-success/80">
              View Results
            </Button>
          )}
        </div>

        {/* Loading State */}
        {showLoading && (
          <Card>
            <CardContent className="p-12 text-center">
              <Loader2 className="h-16 w-16 mx-auto mb-4 text-primary-500 animate-spin" />
              <h3 className="text-xl font-semibold text-white mb-2">
                {scanLogs.some(log => log.message.includes('Generating AI analysis') || log.message.includes('AI analysis') || log.message.includes('🤖')) 
                  ? '🤖 Generating AI Analysis...' 
                  : scanStatus === 'COMPLETED'
                  ? 'Scan Completed - Generating AI Reports...'
                  : 'Scanning in Progress'}
              </h3>
              <p className="text-surface-muted">
                {scanLogs.some(log => log.message.includes('Generating AI analysis') || log.message.includes('AI analysis') || log.message.includes('🤖'))
                  ? 'Gemini AI is analyzing your scan results. Separate reports are being generated for each provider...'
                  : scanStatus === 'COMPLETED'
                  ? 'Scan completed! AI analysis reports are being generated in the background...'
                  : `Analyzing targets with ${formData.providers.join(', ')}...`}
              </p>
              {scanLogs.length > 0 && (
                <div className="mt-6 space-y-2 text-left max-h-64 overflow-y-auto bg-surface-panel p-4 rounded-lg">
                  {scanLogs.slice(-10).map((log, idx) => (
                    <div key={idx} className="text-sm text-surface-muted">
                      <span className="text-primary-500">[{new Date(log.timestamp).toLocaleTimeString()}]</span> {log.message}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Results Summary */}
        {scanResult && scanStatus === 'COMPLETED' && (
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Scan Results</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {/* Display Provider Results */}
                  {scanResult.data && Object.keys(scanResult.data).length > 0 && (
                    <div className="space-y-4">
                      <h3 className="text-lg font-semibold text-white">Provider Results</h3>
                      {Object.entries(scanResult.data).map(([target, providersMap]) => (
                        <div key={target} className="space-y-3">
                          <h4 className="text-white/80 font-medium">Target: {target}</h4>
                          {providersMap && typeof providersMap === 'object' && Object.entries(providersMap).map(([providerName, value]) => {
                            const hasError = value && typeof value === 'object' && 'error' in value;
                            return (
                              <Card key={`${target}-${providerName}`} className={hasError ? 'border-error' : ''}>
                                <CardHeader>
                                  <CardTitle className="text-base flex items-center gap-2">
                                    <span className="px-2 py-0.5 rounded bg-surface-panel border border-surface-border text-white/90">{providerName}</span>
                                    {hasError ? (
                                      <span className="text-error">✗</span>
                                    ) : (
                                      <span className="text-success">✓</span>
                                    )}
                                  </CardTitle>
                                </CardHeader>
                                <CardContent>
                                  {hasError ? (
                                    <div className="p-3 bg-error/10 border border-error/30 rounded text-error">
                                      <p className="font-medium">Error:</p>
                                      <p className="text-sm mt-1">{value.error}</p>
                                    </div>
                                  ) : (
                                    <details open className="bg-surface-panel rounded-lg">
                                      <summary className="cursor-pointer text-surface-muted px-3 py-2">Raw {providerName} payload</summary>
                                      <pre className="p-4 overflow-auto max-h-96 text-sm text-surface-muted">
                                        {JSON.stringify(value, null, 2)}
                                      </pre>
                                    </details>
                                  )}
                                </CardContent>
                              </Card>
                            );
                          })}
                        </div>
                      ))}
                    </div>
                  )}
                  
                  {/* Display Gemini AI Analysis - Provider-specific reports */}
                  <div className="space-y-4 mt-6">
                    <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                      <span className="text-primary-500">🤖</span>
                      AI Security Analysis Reports
                    </h3>
                    
                    {scanResult.data && Object.keys(scanResult.data).map((dataKey) => {
                      const reports = scanResult.gemini_reports || {};
                      const report = reports[dataKey];
                      
                      // Check if report exists and its status
                      if (!report) {
                        return (
                          <Card key={dataKey} className="border-warning/30">
                            <CardHeader>
                              <CardTitle className="text-base flex items-center gap-2">
                                <Loader2 className="h-4 w-4 text-warning animate-spin" />
                                {dataKey} - AI Analysis Pending
                              </CardTitle>
                            </CardHeader>
                            <CardContent>
                              <div className="p-4 bg-warning/10 border border-warning/30 rounded-lg">
                                <p className="text-warning">⏳ AI analysis is queued. Please wait...</p>
                              </div>
                            </CardContent>
                          </Card>
                        );
                      }
                      
                      if (report.status === 'generating') {
                        return (
                          <Card key={dataKey} className="border-warning/30">
                            <CardHeader>
                              <CardTitle className="text-base flex items-center gap-2">
                                <Loader2 className="h-4 w-4 text-warning animate-spin" />
                                {report.provider || dataKey} - {report.target || ''} - Generating AI Analysis...
                              </CardTitle>
                            </CardHeader>
                            <CardContent>
                              <div className="p-4 bg-warning/10 border border-warning/30 rounded-lg">
                                <p className="text-warning flex items-center gap-2">
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                  Gemini AI is analyzing the scan results. This may take a moment...
                                </p>
                              </div>
                            </CardContent>
                          </Card>
                        );
                      }
                      
                      if (report.status === 'failed' || report.has_error) {
                        return (
                          <Card key={dataKey} className="border-error">
                            <CardHeader>
                              <CardTitle className="text-base flex items-center gap-2 text-error">
                                <span>⚠️</span>
                                {report.provider || dataKey} - {report.target || ''} - AI Analysis Failed
                              </CardTitle>
                            </CardHeader>
                            <CardContent>
                              <div className="p-4 bg-error/10 border border-error/30 rounded-lg">
                                <p className="text-error font-medium mb-1">AI Analysis Failed</p>
                                <p className="text-sm text-surface-muted">{report.error || 'Unknown error occurred'}</p>
                              </div>
                            </CardContent>
                          </Card>
                        );
                      }
                      
                      if (report.status === 'completed') {
                        return (
                          <Card key={dataKey} className="border-primary/30">
                            <CardHeader>
                              <CardTitle className="text-base flex items-center gap-2">
                                <span className="text-primary-500">🤖</span>
                                {report.provider || dataKey} - {report.target || ''} - AI Security Analysis
                              </CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-4">
                              {report.analysis ? (
                                <div className="space-y-4">
                                  {/* Summary */}
                                  {report.analysis.summary && (
                                    <Card className="bg-primary/5 border-primary/30">
                                      <CardHeader>
                                        <CardTitle className="text-base">Executive Summary</CardTitle>
                                      </CardHeader>
                                      <CardContent>
                                        <p className="text-surface-muted">{report.analysis.summary}</p>
                                      </CardContent>
                                    </Card>
                                  )}

                                  {/* Risk Assessment */}
                                  {report.analysis.risk_level && (
                                    <div className="grid grid-cols-2 gap-4">
                                      <Card>
                                        <CardHeader>
                                          <CardTitle className="text-base">Risk Level</CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                          <span className={`text-2xl font-bold ${
                                            report.analysis.risk_level === 'CRITICAL' ? 'text-error' :
                                            report.analysis.risk_level === 'HIGH' ? 'text-danger' :
                                            report.analysis.risk_level === 'MEDIUM' ? 'text-warning' :
                                            'text-success'
                                          }`}>
                                            {report.analysis.risk_level}
                                          </span>
                                        </CardContent>
                                      </Card>
                                      {report.analysis.risk_score && (
                                        <Card>
                                          <CardHeader>
                                            <CardTitle className="text-base">Risk Score</CardTitle>
                                          </CardHeader>
                                          <CardContent>
                                            <span className="text-2xl font-bold text-white">
                                              {report.analysis.risk_score}/10
                                            </span>
                                          </CardContent>
                                        </Card>
                                      )}
                                    </div>
                                  )}

                                  {/* Key Findings */}
                                  {report.analysis.key_findings && Array.isArray(report.analysis.key_findings) && report.analysis.key_findings.length > 0 && (
                                    <Card>
                                      <CardHeader>
                                        <CardTitle className="text-base">Key Findings</CardTitle>
                                      </CardHeader>
                                      <CardContent>
                                        <ul className="space-y-2">
                                          {report.analysis.key_findings.map((finding, idx) => (
                                            <li key={idx} className="flex items-start gap-2 text-surface-muted">
                                              <span className="text-primary-500">•</span>
                                              <span>{finding}</span>
                                            </li>
                                          ))}
                                        </ul>
                                      </CardContent>
                                    </Card>
                                  )}

                                  {/* Vulnerabilities */}
                                  {report.analysis.vulnerabilities && Array.isArray(report.analysis.vulnerabilities) && report.analysis.vulnerabilities.length > 0 && (
                                    <Card className="border-danger">
                                      <CardHeader>
                                        <CardTitle className="text-base text-danger">Identified Vulnerabilities</CardTitle>
                                      </CardHeader>
                                      <CardContent>
                                        <ul className="space-y-2">
                                          {report.analysis.vulnerabilities.map((vuln, idx) => (
                                            <li key={idx} className="flex items-start gap-2 text-surface-muted">
                                              <span className="text-danger">⚠</span>
                                              <span>{vuln}</span>
                                            </li>
                                          ))}
                                        </ul>
                                      </CardContent>
                                    </Card>
                                  )}

                                  {/* Recommendations */}
                                  {report.analysis.recommendations && Array.isArray(report.analysis.recommendations) && report.analysis.recommendations.length > 0 && (
                                    <Card className="bg-success/5 border-success/30">
                                      <CardHeader>
                                        <CardTitle className="text-base text-success">Security Recommendations</CardTitle>
                                      </CardHeader>
                                      <CardContent>
                                        <ul className="space-y-2">
                                          {report.analysis.recommendations.map((rec, idx) => (
                                            <li key={idx} className="flex items-start gap-2 text-surface-muted">
                                              <span className="text-success">✓</span>
                                              <span>{rec}</span>
                                            </li>
                                          ))}
                                        </ul>
                                      </CardContent>
                                    </Card>
                                  )}

                                  {/* Threat Indicators */}
                                  {report.analysis.threat_indicators && Array.isArray(report.analysis.threat_indicators) && report.analysis.threat_indicators.length > 0 && (
                                    <Card className="border-warning">
                                      <CardHeader>
                                        <CardTitle className="text-base text-warning">Threat Indicators</CardTitle>
                                      </CardHeader>
                                      <CardContent>
                                        <ul className="space-y-2">
                                          {report.analysis.threat_indicators.map((indicator, idx) => (
                                            <li key={idx} className="flex items-start gap-2 text-surface-muted">
                                              <span className="text-warning">🔍</span>
                                              <span>{indicator}</span>
                                            </li>
                                          ))}
                                        </ul>
                                      </CardContent>
                                    </Card>
                                  )}
                                </div>
                              ) : report.raw_text ? (
                                <Card>
                                  <CardHeader>
                                    <CardTitle className="text-base">AI Analysis</CardTitle>
                                  </CardHeader>
                                  <CardContent>
                                    <pre className="bg-surface-panel p-4 rounded-lg overflow-auto text-sm text-surface-muted whitespace-pre-wrap">
                                      {report.raw_text}
                                    </pre>
                                  </CardContent>
                                </Card>
                              ) : null}
                            </CardContent>
                          </Card>
                        );
                      }
                      
                      return null;
                    })}
                  </div>

                  <div className="flex gap-3">
                    <Button onClick={() => navigate('/dashboard/scans')} variant="outline" className="flex-1">
                      Back to Scans
                    </Button>
                    <Button onClick={handleCloseTerminal} variant="outline" className="flex-1">
                      New Scan
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Error state */}
        {scanResult && scanStatus === 'FAILED' && (
          <Card>
            <CardHeader>
              <CardTitle className="text-error">Scan Failed</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="p-4 bg-error/10 border border-error/30 rounded-lg">
                <p className="text-error font-medium">✗ Scan execution failed</p>
                {scanResult.error && (
                  <p className="text-sm text-surface-muted mt-2">{scanResult.error}</p>
                )}
              </div>
              <Button onClick={handleCloseTerminal} variant="outline" className="w-full mt-4">
                Try Again
              </Button>
            </CardContent>
          </Card>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center space-x-4">
        <Button variant="ghost" onClick={() => navigate('/dashboard/scans')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold text-white">New Scan</h1>
          <p className="text-surface-muted">Create a new security scan</p>
        </div>
      </div>

      {/* Progress steps */}
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            {steps.map((step, index) => (
              <div key={step.number} className="flex items-center">
                <div className={cn(
                  'flex items-center justify-center w-10 h-10 rounded-full border-2',
                  currentStep >= step.number
                    ? 'border-primary-500 bg-primary-500 text-white'
                    : 'border-surface-border text-surface-muted'
                )}>
                  {currentStep > step.number ? (
                    <Check className="h-5 w-5" />
                  ) : (
                    <step.icon className="h-5 w-5" />
                  )}
                </div>
                {index < steps.length - 1 && (
                  <div className={cn(
                    'w-16 h-0.5 mx-4',
                    currentStep > step.number ? 'bg-primary-500' : 'bg-surface-border'
                  )} />
                )}
              </div>
            ))}
          </div>
          <div className="flex justify-between mt-4">
            {steps.map((step) => (
              <span
                key={step.number}
                className={cn(
                  'text-sm',
                  currentStep >= step.number ? 'text-primary-400' : 'text-surface-muted'
                )}
              >
                {step.title}
              </span>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Step content */}
      <Card>
        <CardContent className="p-6">
          {currentStep === 1 && renderStep1()}
          {currentStep === 2 && renderStep2()}
          {currentStep === 3 && renderStep3()}
        </CardContent>
      </Card>

      {/* Navigation */}
      <div className="flex justify-between">
        <Button
          variant="outline"
          onClick={prevStep}
          disabled={currentStep === 1}
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Previous
        </Button>

        <div className="flex space-x-3">
          {currentStep < 3 ? (
            <Button
              onClick={nextStep}
              disabled={!isStepValid()}
            >
              Next
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          ) : (
            <Button
              onClick={handleSubmit}
              disabled={!isStepValid()}
              className="bg-success hover:bg-success/80"
            >
              Create Scan
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default NewScan;
