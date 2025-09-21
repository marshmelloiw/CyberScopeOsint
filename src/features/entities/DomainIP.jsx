import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import RiskBadge from '../../components/common/RiskBadge';
import { Search, Globe, MapPin, Shield, AlertTriangle, Download, ExternalLink, Database, Server, Lock, Eye } from 'lucide-react';
import { cn } from '../../lib/utils';

const DomainIP = () => {
  const [searchTerm, setSearchTerm] = 'google.com';
  const [searchType, setSearchType] = useState('domain');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState(null);

  // Mock data for domain analysis
  const mockDomainData = {
    domain: 'google.com',
    type: 'domain',
    risk: 2,
    reputation: 'good',
    lastUpdated: '2024-01-15T10:00:00Z',
    dns: {
      a: ['142.250.187.78', '142.250.187.110'],
      mx: ['aspmx.l.google.com', 'alt1.aspmx.l.google.com'],
      ns: ['ns1.google.com', 'ns2.google.com'],
      txt: ['google-site-verification=...'],
    },
    whois: {
      registrar: 'MarkMonitor Inc.',
      created: '1997-09-15',
      expires: '2028-09-14',
      status: 'active',
      organization: 'Google LLC',
    },
    security: {
      ssl: true,
      sslGrade: 'A+',
      hsts: true,
      spf: true,
      dmarc: true,
      dkim: true,
    },
    threats: [
      {
        type: 'malware',
        severity: 'low',
        description: 'Historical malware distribution detected',
        source: 'VirusTotal',
        date: '2023-12-01',
      },
    ],
    ports: [
      { port: 80, service: 'HTTP', status: 'open', risk: 'low' },
      { port: 443, service: 'HTTPS', status: 'open', risk: 'low' },
      { port: 22, service: 'SSH', status: 'closed', risk: 'none' },
    ],
  };

  // Mock data for IP analysis
  const mockIPData = {
    ip: '8.8.8.8',
    type: 'ip',
    risk: 1,
    reputation: 'excellent',
    lastUpdated: '2024-01-15T10:00:00Z',
    location: {
      country: 'United States',
      city: 'Mountain View',
      region: 'California',
      coordinates: [37.4056, -122.0775],
      isp: 'Google LLC',
      asn: 'AS15169',
    },
    services: [
      { port: 53, service: 'DNS', status: 'open', risk: 'low' },
      { port: 443, service: 'HTTPS', status: 'open', risk: 'low' },
    ],
    threats: [],
    reputation: {
      abuseScore: 0,
      threatScore: 0,
      trustScore: 100,
    },
  };

  const handleSearch = async () => {
    if (!searchTerm) return;
    
    setIsSearching(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // Return appropriate mock data based on search type
    if (searchType === 'domain') {
      setSearchResults(mockDomainData);
    } else {
      setSearchResults(mockIPData);
    }
    
    setIsSearching(false);
  };

  const getReputationColor = (reputation) => {
    switch (reputation) {
      case 'excellent': return 'text-success';
      case 'good': return 'text-success';
      case 'fair': return 'text-warning';
      case 'poor': return 'text-danger';
      default: return 'text-surface-muted';
    }
  };

  const getReputationBadge = (reputation) => {
    const colors = {
      excellent: 'bg-success/20 text-success border-success/30',
      good: 'bg-success/20 text-success border-success/30',
      fair: 'bg-warning/20 text-warning border-warning/30',
      poor: 'bg-danger/20 text-danger border-danger/30',
    };
    
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${colors[reputation] || 'bg-surface-muted/20 text-surface-muted border-surface-muted/30'}`}>
        {reputation.toUpperCase()}
      </span>
    );
  };

  const getPortRiskColor = (risk) => {
    switch (risk) {
      case 'high': return 'text-danger';
      case 'medium': return 'text-warning';
      case 'low': return 'text-success';
      default: return 'text-surface-muted';
    }
  };

  const renderDomainResults = () => (
    <div className="space-y-6">
      {/* Overview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>Domain Overview</span>
            <div className="flex items-center space-x-3">
              <RiskBadge score={searchResults.risk} />
              {getReputationBadge(searchResults.reputation)}
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <h4 className="font-medium text-white mb-3">Domain Information</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-surface-muted">Domain:</span>
                  <span className="text-white font-mono">{searchResults.domain}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Status:</span>
                  <span className="text-success">Active</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Last Updated:</span>
                  <span className="text-white">{new Date(searchResults.lastUpdated).toLocaleDateString()}</span>
                </div>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-white mb-3">WHOIS Details</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-surface-muted">Registrar:</span>
                  <span className="text-white">{searchResults.whois.registrar}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Created:</span>
                  <span className="text-white">{new Date(searchResults.whois.created).toLocaleDateString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Expires:</span>
                  <span className="text-white">{new Date(searchResults.whois.expires).toLocaleDateString()}</span>
                </div>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-white mb-3">Security Status</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-surface-muted">SSL:</span>
                  <span className={searchResults.security.ssl ? 'text-success' : 'text-danger'}>
                    {searchResults.security.ssl ? '✓' : '✗'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">HSTS:</span>
                  <span className={searchResults.security.hsts ? 'text-success' : 'text-danger'}>
                    {searchResults.security.hsts ? '✓' : '✗'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">SPF:</span>
                  <span className={searchResults.security.spf ? 'text-success' : 'text-danger'}>
                    {searchResults.security.spf ? '✓' : '✗'}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* DNS Records */}
      <Card>
        <CardHeader>
          <CardTitle>DNS Records</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h5 className="font-medium text-white mb-2">A Records</h5>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                {searchResults.dns.a.map((ip, idx) => (
                  <div key={idx} className="p-2 bg-surface-panel/50 rounded text-sm font-mono text-white">
                    {ip}
                  </div>
                ))}
              </div>
            </div>
            
            <div>
              <h5 className="font-medium text-white mb-2">MX Records</h5>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                {searchResults.dns.mx.map((mx, idx) => (
                  <div key={idx} className="p-2 bg-surface-panel/50 rounded text-sm font-mono text-white">
                    {mx}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Ports & Services */}
      <Card>
        <CardHeader>
          <CardTitle>Ports & Services</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-surface-border">
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Port</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Service</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Status</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Risk</th>
                </tr>
              </thead>
              <tbody>
                {searchResults.ports.map((port, idx) => (
                  <tr key={idx} className="border-b border-surface-border/50">
                    <td className="p-2">
                      <span className="font-mono text-white">{port.port}</span>
                    </td>
                    <td className="p-2 text-white">{port.service}</td>
                    <td className="p-2">
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs',
                        port.status === 'open' ? 'bg-success/20 text-success' : 'bg-surface-muted/20 text-surface-muted'
                      )}>
                        {port.status}
                      </span>
                    </td>
                    <td className="p-2">
                      <span className={cn('text-xs', getPortRiskColor(port.risk))}>
                        {port.risk.toUpperCase()}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Threats */}
      {searchResults.threats.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <AlertTriangle className="h-5 w-5 text-warning" />
              <span>Threats Detected</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {searchResults.threats.map((threat, idx) => (
                <div key={idx} className="p-3 bg-warning/10 border border-warning/20 rounded-lg">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-medium text-white">{threat.type}</h4>
                      <p className="text-sm text-surface-muted">{threat.description}</p>
                      <p className="text-xs text-surface-muted mt-1">
                        Source: {threat.source} • Date: {new Date(threat.date).toLocaleDateString()}
                      </p>
                    </div>
                    <span className={cn(
                      'px-2 py-1 rounded-full text-xs font-medium',
                      threat.severity === 'high' ? 'bg-danger/20 text-danger' : 'bg-warning/20 text-warning'
                    )}>
                      {threat.severity.toUpperCase()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );

  const renderIPResults = () => (
    <div className="space-y-6">
      {/* Overview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>IP Address Overview</span>
            <div className="flex items-center space-x-3">
              <RiskBadge score={searchResults.risk} />
              {getReputationBadge(searchResults.reputation)}
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="font-medium text-white mb-3">IP Information</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-surface-muted">IP Address:</span>
                  <span className="text-white font-mono">{searchResults.ip}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Risk Level:</span>
                  <span className="text-success">Low</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">Last Updated:</span>
                  <span className="text-white">{new Date(searchResults.lastUpdated).toLocaleDateString()}</span>
                </div>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-white mb-3">Location & Network</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-surface-muted">Country:</span>
                  <span className="text-white">{searchResults.location.country}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">City:</span>
                  <span className="text-white">{searchResults.location.city}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-surface-muted">ISP:</span>
                  <span className="text-white">{searchResults.location.isp}</span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Reputation Scores */}
      <Card>
        <CardHeader>
          <CardTitle>Reputation Scores</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
              <p className="text-2xl font-bold text-success">{searchResults.reputation.abuseScore}</p>
              <p className="text-sm text-surface-muted">Abuse Score</p>
              <p className="text-xs text-success">(Lower is better)</p>
            </div>
            <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
              <p className="text-2xl font-bold text-success">{searchResults.reputation.threatScore}</p>
              <p className="text-sm text-surface-muted">Threat Score</p>
              <p className="text-xs text-success">(Lower is better)</p>
            </div>
            <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
              <p className="text-2xl font-bold text-success">{searchResults.reputation.trustScore}</p>
              <p className="text-sm text-surface-muted">Trust Score</p>
              <p className="text-xs text-success">(Higher is better)</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Services */}
      <Card>
        <CardHeader>
          <CardTitle>Active Services</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-surface-border">
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Port</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Service</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Status</th>
                  <th className="text-left p-2 text-sm font-medium text-surface-muted">Risk</th>
                </tr>
              </thead>
              <tbody>
                {searchResults.services.map((service, idx) => (
                  <tr key={idx} className="border-b border-surface-border/50">
                    <td className="p-2">
                      <span className="font-mono text-white">{service.port}</span>
                    </td>
                    <td className="p-2 text-white">{service.service}</td>
                    <td className="p-2">
                      <span className="px-2 py-1 rounded-full text-xs bg-success/20 text-success">
                        {service.status}
                      </span>
                    </td>
                    <td className="p-2">
                      <span className="text-xs text-success">LOW</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Domain & IP Analysis</h1>
        <p className="text-surface-muted">Analyze domains and IP addresses for security threats and vulnerabilities</p>
      </div>

      {/* Search section */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            {searchType === 'domain' ? <Globe className="h-5 w-5" /> : <MapPin className="h-5 w-5" />}
            <span>Analyze {searchType === 'domain' ? 'Domain' : 'IP Address'}</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex space-x-3">
              <div className="flex-1">
                <Input
                  placeholder={searchType === 'domain' ? 'Enter domain (e.g., google.com)' : 'Enter IP address (e.g., 8.8.8.8)'}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                />
              </div>
              <Button
                onClick={handleSearch}
                disabled={!searchTerm || isSearching}
                loading={isSearching}
              >
                <Search className="h-4 w-4 mr-2" />
                {isSearching ? 'Analyzing...' : 'Analyze'}
              </Button>
            </div>

            <div className="flex items-center space-x-4">
              <label className="flex items-center space-x-2">
                <input
                  type="radio"
                  name="searchType"
                  value="domain"
                  checked={searchType === 'domain'}
                  onChange={(e) => setSearchType(e.target.value)}
                  className="text-primary-600"
                />
                <span className="text-white">Domain</span>
              </label>
              <label className="flex items-center space-x-2">
                <input
                  type="radio"
                  name="searchType"
                  value="ip"
                  checked={searchType === 'ip'}
                  onChange={(e) => setSearchType(e.target.value)}
                  className="text-primary-600"
                />
                <span className="text-white">IP Address</span>
              </label>
            </div>

            <p className="text-sm text-surface-muted">
              We'll analyze the {searchType} for security threats, vulnerabilities, and reputation using multiple intelligence sources
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      {searchResults && (
        <div className="space-y-6">
          {searchResults.type === 'domain' ? renderDomainResults() : renderIPResults()}
        </div>
      )}

      {/* Information about the service */}
      <Card>
        <CardHeader>
          <CardTitle>About This Service</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3 text-sm text-surface-muted">
            <p>
              Our domain and IP analysis service provides comprehensive security assessment using multiple 
              intelligence sources including VirusTotal, Shodan, WHOIS databases, and threat intelligence feeds.
            </p>
            <p>
              <strong>Data Sources:</strong> VirusTotal, Shodan, WHOIS, AbuseIPDB, URLVoid, and more
            </p>
            <p>
              <strong>Privacy:</strong> Your search queries are not logged and results are not stored
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default DomainIP;
