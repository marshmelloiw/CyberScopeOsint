import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Search, Plus, FileText, Download, Eye, Calendar, Filter, BarChart3, Users, Globe, Mail, MapPin } from 'lucide-react';
import { cn } from '../../lib/utils';

const Reports = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [showReportBuilder, setShowReportBuilder] = useState(false);

  // Mock reports data
  const reports = [
    {
      id: 1,
      title: 'Monthly Security Assessment - January 2024',
      type: 'security-assessment',
      status: 'completed',
      createdAt: '2024-01-15T08:00:00Z',
      updatedAt: '2024-01-15T10:00:00Z',
      author: 'Melisa Bayramoğlu',
      scope: 'All company domains',
      findings: 23,
      riskScore: 6,
      format: 'PDF',
      size: '2.4 MB',
      tags: ['monthly', 'security', 'assessment'],
    },
    {
      id: 2,
      title: 'Domain Security Report - google.com',
      type: 'domain-analysis',
      status: 'completed',
      createdAt: '2024-01-14T14:00:00Z',
      updatedAt: '2024-01-14T14:30:00Z',
      author: 'Melisa Bayramoğlu',
      scope: 'google.com',
      findings: 12,
      riskScore: 3,
      format: 'HTML',
      size: '1.8 MB',
      tags: ['domain', 'google', 'security'],
    },
    {
      id: 3,
      title: 'Email Breach Analysis - Q4 2023',
      type: 'breach-analysis',
      status: 'draft',
      createdAt: '2024-01-13T09:00:00Z',
      updatedAt: '2024-01-13T09:00:00Z',
      author: 'Melisa Bayramoğlu',
      scope: 'Employee emails',
      findings: 45,
      riskScore: 8,
      format: 'PDF',
      size: '3.1 MB',
      tags: ['quarterly', 'breach', 'email'],
    },
    {
      id: 4,
      title: 'IP Threat Intelligence Report',
      type: 'threat-intel',
      status: 'in-progress',
      createdAt: '2024-01-12T11:00:00Z',
      updatedAt: '2024-01-15T09:00:00Z',
      author: 'Melisa Bayramoğlu',
      scope: 'Network IPs',
      findings: 18,
      riskScore: 4,
      format: 'PDF',
      size: '2.7 MB',
      tags: ['threat', 'intelligence', 'network'],
    },
  ];

  const reportTypes = [
    { id: 'security-assessment', name: 'Security Assessment', icon: Shield, description: 'Comprehensive security evaluation' },
    { id: 'domain-analysis', name: 'Domain Analysis', icon: Globe, description: 'Domain security and reputation analysis' },
    { id: 'breach-analysis', name: 'Breach Analysis', icon: Mail, description: 'Data breach investigation and findings' },
    { id: 'threat-intel', name: 'Threat Intelligence', icon: AlertTriangle, description: 'Threat intelligence and indicators' },
    { id: 'vulnerability-scan', name: 'Vulnerability Scan', icon: Search, description: 'Vulnerability assessment results' },
    { id: 'compliance-report', name: 'Compliance Report', icon: CheckCircle, description: 'Regulatory compliance assessment' },
  ];

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed': return 'bg-success text-white';
      case 'in-progress': return 'bg-info text-white';
      case 'draft': return 'bg-warning text-white';
      case 'failed': return 'bg-danger text-white';
      default: return 'bg-surface-muted text-white';
    }
  };

  const getTypeIcon = (type) => {
    const reportType = reportTypes.find(t => t.id === type);
    if (reportType) {
      const IconComponent = reportType.icon;
      return <IconComponent className="h-5 w-5" />;
    }
    return <FileText className="h-5 w-5" />;
  };

  const filteredReports = reports.filter(report => {
    const matchesSearch = report.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         report.author.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         report.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'all' || report.status === statusFilter;
    const matchesType = typeFilter === 'all' || report.type === typeFilter;
    
    return matchesSearch && matchesStatus && matchesType;
  });

  const ReportBuilder = () => (
    <Card>
      <CardHeader>
        <CardTitle>Create New Report</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {/* Basic Information */}
          <div>
            <h4 className="font-medium text-white mb-3">Basic Information</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-white mb-2">Report Title</label>
                <Input placeholder="Enter report title..." />
              </div>
              <div>
                <label className="block text-sm font-medium text-white mb-2">Report Type</label>
                <select className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent">
                  <option value="">Select report type</option>
                  {reportTypes.map((type) => (
                    <option key={type.id} value={type.id}>{type.name}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Scope and Content */}
          <div>
            <h4 className="font-medium text-white mb-3">Scope and Content</h4>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-white mb-2">Scope Description</label>
                <textarea
                  placeholder="Describe what this report covers..."
                  className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
                  rows={3}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-white mb-2">Tags</label>
                <Input placeholder="Enter tags separated by commas..." />
              </div>
            </div>
          </div>

          {/* Output Options */}
          <div>
            <h4 className="font-medium text-white mb-3">Output Options</h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-white mb-2">Format</label>
                <select className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent">
                  <option value="pdf">PDF</option>
                  <option value="html">HTML</option>
                  <option value="docx">Word Document</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-white mb-2">Include Charts</label>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="includeCharts"
                    defaultChecked
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                  />
                  <label htmlFor="includeCharts" className="text-white">Yes</label>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-white mb-2">Include Executive Summary</label>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="includeSummary"
                    defaultChecked
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                  />
                  <label htmlFor="includeSummary" className="text-white">Yes</label>
                </div>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowReportBuilder(false)}>
              Cancel
            </Button>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              Create Report
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Reports</h1>
          <p className="text-surface-muted">Generate and manage security reports</p>
        </div>
        <Button onClick={() => setShowReportBuilder(true)} className="flex items-center space-x-2">
          <Plus className="h-4 w-4" />
          <span>New Report</span>
        </Button>
      </div>

      {/* Report Builder */}
      {showReportBuilder && <ReportBuilder />}

      {/* Filters and search */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-muted" />
                <Input
                  placeholder="Search reports by title, author, or tags..."
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
              <option value="in-progress">In Progress</option>
              <option value="draft">Draft</option>
              <option value="failed">Failed</option>
            </select>

            {/* Type filter */}
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="all">All Types</option>
              {reportTypes.map((type) => (
                <option key={type.id} value={type.id}>{type.name}</option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Reports list */}
      <Card>
        <CardHeader>
          <CardTitle>Reports ({filteredReports.length})</CardTitle>
        </CardHeader>
        <CardContent>
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
                        'px-2 py-1 rounded-full text-xs font-medium',
                        getStatusColor(report.status)
                      )}>
                        {report.status}
                      </span>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-surface-muted">Author:</span>
                        <span className="text-white ml-2">{report.author}</span>
                      </div>
                      <div>
                        <span className="text-surface-muted">Scope:</span>
                        <span className="text-white ml-2">{report.scope}</span>
                      </div>
                      <div>
                        <span className="text-surface-muted">Findings:</span>
                        <span className="text-white ml-2">{report.findings}</span>
                      </div>
                      <div>
                        <span className="text-surface-muted">Risk Score:</span>
                        <span className="text-white ml-2">{report.riskScore}/10</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4 mt-3 text-sm text-surface-muted">
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>Created: {new Date(report.createdAt).toLocaleDateString()}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <FileText className="h-4 w-4" />
                        <span>{report.format} • {report.size}</span>
                      </div>
                    </div>
                    
                    <div className="flex flex-wrap gap-1 mt-3">
                      {report.tags.map((tag, idx) => (
                        <span
                          key={idx}
                          className="px-2 py-1 bg-surface-panel text-xs text-surface-muted rounded"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                  
                  <div className="flex flex-col space-y-2 ml-4">
                    <Button variant="ghost" size="sm">
                      <Eye className="h-4 w-4 mr-2" />
                      View
                    </Button>
                    <Button variant="ghost" size="sm">
                      <Download className="h-4 w-4 mr-2" />
                      Download
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <BarChart3 className="h-6 w-6" />
              <span>Generate Monthly Report</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Users className="h-6 w-6" />
              <span>Team Activity Report</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Globe className="h-6 w-6" />
              <span>Domain Security Summary</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Reports;
