import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import RiskBadge from '../../components/common/RiskBadge';
import { Search, Plus, Filter, Download, Eye, Play, Pause, Trash2 } from 'lucide-react';
import { cn } from '../../lib/utils';

const ScansList = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');

  // Mock data
  const scans = [
    {
      id: 1,
      name: 'Domain Security Assessment - google.com',
      type: 'domain',
      targets: ['google.com'],
      status: 'completed',
      risk: 3,
      startedAt: '2024-01-15T08:00:00Z',
      finishedAt: '2024-01-15T08:05:00Z',
      findings: 12,
      providers: ['VirusTotal', 'Shodan', 'Whois'],
    },
    {
      id: 2,
      name: 'Email Breach Check - test@example.com',
      type: 'email',
      targets: ['test@example.com'],
      status: 'completed',
      risk: 7,
      startedAt: '2024-01-15T07:00:00Z',
      finishedAt: '2024-01-15T07:02:00Z',
      findings: 3,
      providers: ['HaveIBeenPwned'],
    },
    {
      id: 3,
      name: 'IP Address Analysis - 8.8.8.8',
      type: 'ip',
      targets: ['8.8.8.8'],
      status: 'running',
      risk: 2,
      startedAt: '2024-01-15T09:00:00Z',
      finishedAt: null,
      findings: 0,
      providers: ['Shodan', 'VirusTotal'],
    },
    {
      id: 4,
      name: 'Social Media Monitor - @cyberscope',
      type: 'social',
      targets: ['@cyberscope'],
      status: 'queued',
      risk: 0,
      startedAt: null,
      finishedAt: null,
      findings: 0,
      providers: ['Twitter', 'LinkedIn'],
    },
  ];

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

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Scans</h1>
          <p className="text-surface-muted">Monitor and manage your security scans</p>
        </div>
        <Link to="/scans/new">
          <Button className="flex items-center space-x-2">
            <Plus className="h-4 w-4" />
            <span>New Scan</span>
          </Button>
        </Link>
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
              <option value="social">Social Media</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Scans table */}
      <Card>
        <CardHeader>
          <CardTitle>Scan History</CardTitle>
        </CardHeader>
        <CardContent>
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
                  <tr key={scan.id} className="border-b border-surface-border/50 hover:bg-surface-panel/50">
                    <td className="p-3">
                      <div>
                        <p className="font-medium text-white">{scan.name}</p>
                        <p className="text-sm text-surface-muted">
                          Targets: {scan.targets.join(', ')}
                        </p>
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center space-x-2">
                        <span className="text-lg">{getTypeIcon(scan.type)}</span>
                        <span className="capitalize text-white">{scan.type}</span>
                      </div>
                    </td>
                    <td className="p-3">
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs font-medium',
                        getStatusColor(scan.status)
                      )}>
                        {scan.status}
                      </span>
                    </td>
                    <td className="p-3">
                      <RiskBadge score={scan.risk} />
                    </td>
                    <td className="p-3">
                      <span className="text-white">{scan.findings}</span>
                    </td>
                    <td className="p-3">
                      <span className="text-surface-muted">
                        {scan.startedAt ? new Date(scan.startedAt).toLocaleDateString() : '-'}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center space-x-2">
                        <Link to={`/scans/${scan.id}`}>
                          <Button variant="ghost" size="sm">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </Link>
                        {scan.status === 'running' && (
                          <Button variant="ghost" size="sm">
                            <Pause className="h-4 w-4" />
                          </Button>
                        )}
                        {scan.status === 'queued' && (
                          <Button variant="ghost" size="sm">
                            <Play className="h-4 w-4" />
                          </Button>
                        )}
                        <Button variant="ghost" size="sm">
                          <Download className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm" className="text-danger hover:text-danger">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
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
};

export default ScansList;
