import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Search, Mail, AlertTriangle, Shield, Download, ExternalLink, Calendar, Database } from 'lucide-react';

const EmailBreach = () => {
  const [email, setEmail] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState(null);

  // Mock breach data
  const mockBreachData = {
    email: 'test@example.com',
    found: true,
    breaches: [
      {
        name: 'Adobe',
        date: '2013-10-04',
        records: 153000000,
        severity: 'high',
        description: 'Adobe suffered a massive data breach that exposed user credentials and personal information.',
        dataTypes: ['email addresses', 'password hints', 'names', 'phone numbers'],
        verified: true,
      },
      {
        name: 'LinkedIn',
        date: '2012-05-05',
        records: 117000000,
        severity: 'medium',
        description: 'LinkedIn experienced a data breach where user account credentials were compromised.',
        dataTypes: ['email addresses', 'passwords', 'names'],
        verified: true,
      },
      {
        name: 'Dropbox',
        date: '2012-07-01',
        records: 68700000,
        severity: 'low',
        description: 'Dropbox user credentials were exposed in a data breach.',
        dataTypes: ['email addresses', 'passwords'],
        verified: true,
      },
    ],
    summary: {
      totalBreaches: 3,
      totalRecords: 338700000,
      lastSeen: '2013-10-04',
      riskScore: 7,
    }
  };

  const handleSearch = async () => {
    if (!email) return;
    
    setIsSearching(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    setSearchResults(mockBreachData);
    setIsSearching(false);
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'high': return 'text-danger';
      case 'medium': return 'text-warning';
      case 'low': return 'text-success';
      default: return 'text-surface-muted';
    }
  };

  const getSeverityBadge = (severity) => {
    const colors = {
      high: 'bg-danger/20 text-danger border-danger/30',
      medium: 'bg-warning/20 text-warning border-warning/30',
      low: 'bg-success/20 text-success border-success/30',
    };
    
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${colors[severity]}`}>
        {severity.toUpperCase()}
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Email Breach Analysis</h1>
        <p className="text-surface-muted">Check if email addresses have been compromised in data breaches</p>
      </div>

      {/* Search section */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Mail className="h-5 w-5" />
            <span>Check Email Address</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex space-x-3">
            <div className="flex-1">
              <Input
                type="email"
                placeholder="Enter email address to check..."
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <Button
              onClick={handleSearch}
              disabled={!email || isSearching}
              loading={isSearching}
            >
              <Search className="h-4 w-4 mr-2" />
              {isSearching ? 'Checking...' : 'Check'}
            </Button>
          </div>
          <p className="text-sm text-surface-muted mt-2">
            We'll check against multiple breach databases to see if this email has been compromised
          </p>
        </CardContent>
      </Card>

      {/* Results */}
      {searchResults && (
        <div className="space-y-6">
          {/* Summary card */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>Breach Summary</span>
                {searchResults.found ? (
                  <div className="flex items-center space-x-2">
                    <AlertTriangle className="h-5 w-5 text-danger" />
                    <span className="text-danger">BREACHED</span>
                  </div>
                ) : (
                  <div className="flex items-center space-x-2">
                    <Shield className="h-5 w-5 text-success" />
                    <span className="text-success">SAFE</span>
                  </div>
                )}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
                  <p className="text-2xl font-bold text-white">{searchResults.summary.totalBreaches}</p>
                  <p className="text-sm text-surface-muted">Total Breaches</p>
                </div>
                <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
                  <p className="text-2xl font-bold text-white">
                    {searchResults.summary.totalRecords.toLocaleString()}
                  </p>
                  <p className="text-sm text-surface-muted">Records Exposed</p>
                </div>
                <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
                  <p className="text-2xl font-bold text-white">{searchResults.summary.riskScore}/10</p>
                  <p className="text-sm text-surface-muted">Risk Score</p>
                </div>
                <div className="text-center p-4 bg-surface-panel/50 rounded-lg">
                  <p className="text-2xl font-bold text-white">
                    {new Date(searchResults.summary.lastSeen).getFullYear()}
                  </p>
                  <p className="text-sm text-surface-muted">Last Breach</p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Breaches list */}
          <Card>
            <CardHeader>
              <CardTitle>Breach Details</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {searchResults.breaches.map((breach, index) => (
                  <div
                    key={index}
                    className="p-4 border border-surface-border rounded-lg hover:bg-surface-panel/50 transition-colors"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-3 mb-2">
                          <h3 className="text-lg font-semibold text-white">{breach.name}</h3>
                          {getSeverityBadge(breach.severity)}
                          {breach.verified && (
                            <span className="px-2 py-1 bg-success/20 text-success text-xs rounded-full">
                              VERIFIED
                            </span>
                          )}
                        </div>
                        
                        <p className="text-surface-muted mb-3">{breach.description}</p>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <p className="text-sm text-surface-muted mb-1">Data Types Exposed:</p>
                            <div className="flex flex-wrap gap-1">
                              {breach.dataTypes.map((type, idx) => (
                                <span
                                  key={idx}
                                  className="px-2 py-1 bg-surface-panel text-xs text-surface-muted rounded"
                                >
                                  {type}
                                </span>
                              ))}
                            </div>
                          </div>
                          
                          <div className="space-y-2">
                            <div className="flex justify-between">
                              <span className="text-sm text-surface-muted">Records:</span>
                              <span className="text-white font-medium">
                                {breach.records.toLocaleString()}
                              </span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-sm text-surface-muted">Date:</span>
                              <span className="text-white">
                                {new Date(breach.date).toLocaleDateString()}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                      
                      <div className="flex flex-col space-y-2 ml-4">
                        <Button variant="outline" size="sm">
                          <Download className="h-4 w-4 mr-2" />
                          Export
                        </Button>
                        <Button variant="outline" size="sm">
                          <ExternalLink className="h-4 w-4 mr-2" />
                          Details
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Recommendations */}
          <Card>
            <CardHeader>
              <CardTitle>Security Recommendations</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-start space-x-3 p-3 bg-danger/10 border border-danger/20 rounded-lg">
                  <AlertTriangle className="h-5 w-5 text-danger mt-0.5" />
                  <div>
                    <p className="font-medium text-white">Change Passwords</p>
                    <p className="text-sm text-surface-muted">
                      If you used the same password on {searchResults.email}, change it immediately on all accounts.
                    </p>
                  </div>
                </div>
                
                <div className="flex items-start space-x-3 p-3 bg-warning/10 border border-warning/20 rounded-lg">
                  <Shield className="h-5 w-5 text-warning mt-0.5" />
                  <div>
                    <p className="font-medium text-white">Enable 2FA</p>
                    <p className="text-sm text-surface-muted">
                      Enable two-factor authentication on all your accounts for additional security.
                    </p>
                  </div>
                </div>
                
                <div className="flex items-start space-x-3 p-3 bg-info/10 border border-info/20 rounded-lg">
                  <Database className="h-5 w-5 text-info mt-0.5" />
                  <div>
                    <p className="font-medium text-white">Monitor Accounts</p>
                    <p className="text-sm text-surface-muted">
                      Regularly check your accounts for suspicious activity and enable security alerts.
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
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
              Our email breach analysis service checks against multiple databases including HaveIBeenPwned, 
              DeHashed, and other verified breach sources. We only check if an email appears in breaches, 
              we never store or transmit the actual breached data.
            </p>
            <p>
              <strong>Data Sources:</strong> HaveIBeenPwned, DeHashed, Intelligence X, LeakCheck, and more
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

export default EmailBreach;
