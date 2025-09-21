import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Key, Plus, Copy, Eye, EyeOff, Trash2, RefreshCw, Download, Upload, Calendar, Globe, Shield, Activity } from 'lucide-react';
import { cn } from '../../lib/utils';

const APIKeys = () => {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showSecret, setShowSecret] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  // Mock API keys data
  const apiKeys = [
    {
      id: 1,
      name: 'Production API Key',
      key: 'cy_sk_prod_1234567890abcdef',
      secret: 'cy_sk_prod_secret_abcdef1234567890',
      status: 'active',
      permissions: ['read', 'write', 'scan', 'reports'],
      createdAt: '2024-01-10T08:00:00Z',
      lastUsed: '2024-01-15T14:30:00Z',
      usageCount: 15420,
      rateLimit: '1000/hour',
      expiresAt: null,
      description: 'Primary API key for production integrations',
    },
    {
      id: 2,
      name: 'Development API Key',
      key: 'cy_sk_dev_9876543210fedcba',
      secret: 'cy_sk_dev_secret_fedcba0987654321',
      status: 'active',
      permissions: ['read', 'scan'],
      createdAt: '2024-01-12T10:00:00Z',
      lastUsed: '2024-01-15T09:15:00Z',
      usageCount: 2340,
      rateLimit: '100/hour',
      expiresAt: '2024-04-12T10:00:00Z',
      description: 'API key for development and testing',
    },
    {
      id: 3,
      name: 'Read-Only API Key',
      key: 'cy_sk_read_abcdef1234567890',
      secret: 'cy_sk_read_secret_1234567890abcdef',
      status: 'active',
      permissions: ['read'],
      createdAt: '2024-01-08T14:00:00Z',
      lastUsed: '2024-01-14T16:45:00Z',
      usageCount: 890,
      rateLimit: '500/hour',
      expiresAt: null,
      description: 'Read-only access for reporting tools',
    },
    {
      id: 4,
      name: 'Expired API Key',
      key: 'cy_sk_exp_1234567890abcdef',
      secret: 'cy_sk_exp_secret_abcdef1234567890',
      status: 'expired',
      permissions: ['read', 'write'],
      createdAt: '2023-12-01T08:00:00Z',
      lastUsed: '2023-12-31T23:59:00Z',
      usageCount: 5670,
      rateLimit: '1000/hour',
      expiresAt: '2023-12-31T23:59:00Z',
      description: 'Old API key that has expired',
    },
  ];

  const [newKeyData, setNewKeyData] = useState({
    name: '',
    description: '',
    permissions: [],
    expiresAt: '',
    rateLimit: '1000/hour',
  });

  const permissions = [
    { id: 'read', label: 'Read Access', description: 'View data and reports' },
    { id: 'write', label: 'Write Access', description: 'Create and update data' },
    { id: 'scan', label: 'Scan Access', description: 'Initiate security scans' },
    { id: 'reports', label: 'Reports Access', description: 'Generate and export reports' },
    { id: 'admin', label: 'Admin Access', description: 'Full system access' },
  ];

  const rateLimits = [
    '100/hour',
    '500/hour',
    '1000/hour',
    '5000/hour',
    '10000/hour',
    'Unlimited',
  ];

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-success/20 text-success border-success/30';
      case 'inactive': return 'bg-surface-muted/20 text-surface-muted border-surface-muted/30';
      case 'expired': return 'bg-danger/20 text-danger border-danger/30';
      case 'suspended': return 'bg-warning/20 text-warning border-warning/30';
      default: return 'bg-surface-muted/20 text-surface-muted border-surface-muted/30';
    }
  };

  const getPermissionColor = (permission) => {
    switch (permission) {
      case 'read': return 'bg-info/20 text-info';
      case 'write': return 'bg-warning/20 text-warning';
      case 'scan': return 'bg-success/20 text-success';
      case 'reports': return 'bg-primary/20 text-primary';
      case 'admin': return 'bg-danger/20 text-danger';
      default: return 'bg-surface-muted/20 text-surface-muted';
    }
  };

  const filteredKeys = apiKeys.filter(key => {
    const matchesSearch = key.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         key.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || key.status === statusFilter;
    
    return matchesSearch && matchesStatus;
  });

  const handleCreateKey = () => {
    if (!newKeyData.name || newKeyData.permissions.length === 0) {
      alert('Please provide a name and select at least one permission');
      return;
    }

    // Mock API call
    console.log('Creating new API key:', newKeyData);
    
    // Reset form
    setNewKeyData({
      name: '',
      description: '',
      permissions: [],
      expiresAt: '',
      rateLimit: '1000/hour',
    });
    setShowCreateForm(false);
  };

  const handlePermissionToggle = (permission) => {
    setNewKeyData(prev => ({
      ...prev,
      permissions: prev.permissions.includes(permission)
        ? prev.permissions.filter(p => p !== permission)
        : [...prev.permissions, permission]
    }));
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    // In a real app, you'd show a toast notification
    console.log('Copied to clipboard:', text);
  };

  const revokeKey = (id) => {
    if (confirm('Are you sure you want to revoke this API key? This action cannot be undone.')) {
      console.log('Revoking API key:', id);
    }
  };

  const regenerateKey = (id) => {
    if (confirm('Are you sure you want to regenerate this API key? The old key will be invalidated.')) {
      console.log('Regenerating API key:', id);
    }
  };

  const CreateKeyForm = () => (
    <Card>
      <CardHeader>
        <CardTitle>Create New API Key</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white mb-2">Key Name</label>
              <Input
                placeholder="e.g., Production API Key"
                value={newKeyData.name}
                onChange={(e) => setNewKeyData(prev => ({ ...prev, name: e.target.value }))}
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Rate Limit</label>
              <select
                value={newKeyData.rateLimit}
                onChange={(e) => setNewKeyData(prev => ({ ...prev, rateLimit: e.target.value }))}
                className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                {rateLimits.map((limit) => (
                  <option key={limit} value={limit}>{limit}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Description</label>
            <textarea
              placeholder="Describe what this API key will be used for..."
              value={newKeyData.description}
              onChange={(e) => setNewKeyData(prev => ({ ...prev, description: e.target.value }))}
              className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              rows={3}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Expiration Date (Optional)</label>
            <Input
              type="datetime-local"
              value={newKeyData.expiresAt}
              onChange={(e) => setNewKeyData(prev => ({ ...prev, expiresAt: e.target.value }))}
            />
          </div>

          {/* Permissions */}
          <div>
            <label className="block text-sm font-medium text-white mb-3">Permissions</label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {permissions.map((permission) => (
                <div
                  key={permission.id}
                  className={cn(
                    'p-3 border rounded-lg cursor-pointer transition-all',
                    newKeyData.permissions.includes(permission.id)
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-border hover:border-primary-400'
                  )}
                  onClick={() => handlePermissionToggle(permission.id)}
                >
                  <div className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      checked={newKeyData.permissions.includes(permission.id)}
                      onChange={() => {}} // Controlled by onClick
                      className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                    />
                    <div>
                      <p className="font-medium text-white">{permission.label}</p>
                      <p className="text-sm text-surface-muted">{permission.description}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowCreateForm(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateKey}>
              <Plus className="h-4 w-4 mr-2" />
              Create API Key
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
          <h1 className="text-3xl font-bold text-white">API Keys</h1>
          <p className="text-surface-muted">Manage your API keys and integrations</p>
        </div>
        <Button onClick={() => setShowCreateForm(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New API Key
        </Button>
      </div>

      {/* Create Key Form */}
      {showCreateForm && <CreateKeyForm />}

      {/* Filters and search */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Key className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-muted" />
                <Input
                  placeholder="Search API keys by name or description..."
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
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="expired">Expired</option>
              <option value="suspended">Suspended</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* API Keys list */}
      <Card>
        <CardHeader>
          <CardTitle>API Keys ({filteredKeys.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {filteredKeys.map((key) => (
              <div
                key={key.id}
                className="p-4 border border-surface-border rounded-lg hover:bg-surface-panel/50 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <Key className="h-5 w-5 text-primary" />
                      <h3 className="text-lg font-semibold text-white">{key.name}</h3>
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs font-medium border',
                        getStatusColor(key.status)
                      )}>
                        {key.status.toUpperCase()}
                      </span>
                    </div>
                    
                    <p className="text-surface-muted mb-3">{key.description}</p>
                    
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-surface-muted">API Key:</span>
                        <div className="flex items-center space-x-2 mt-1">
                          <span className="font-mono text-white">{key.key}</span>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => copyToClipboard(key.key)}
                          >
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-surface-muted">Secret:</span>
                        <div className="flex items-center space-x-2 mt-1">
                          <span className="font-mono text-white">
                            {showSecret[key.id] ? key.secret : '••••••••••••••••••••'}
                          </span>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setShowSecret(prev => ({ ...prev, [key.id]: !prev[key.id] }))}
                          >
                            {showSecret[key.id] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => copyToClipboard(key.secret)}
                          >
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-surface-muted">Rate Limit:</span>
                        <span className="text-white ml-2">{key.rateLimit}</span>
                      </div>
                      
                      <div>
                        <span className="text-surface-muted">Usage Count:</span>
                        <span className="text-white ml-2">{key.usageCount.toLocaleString()}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4 mt-3 text-sm text-surface-muted">
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>Created: {new Date(key.createdAt).toLocaleDateString()}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <Activity className="h-4 w-4" />
                        <span>Last Used: {new Date(key.lastUsed).toLocaleDateString()}</span>
                      </div>
                      {key.expiresAt && (
                        <div className="flex items-center space-x-1">
                          <Globe className="h-4 w-4" />
                          <span>Expires: {new Date(key.expiresAt).toLocaleDateString()}</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="flex flex-wrap gap-1 mt-3">
                      {key.permissions.map((permission) => (
                        <span
                          key={permission}
                          className={cn(
                            'px-2 py-1 text-xs rounded',
                            getPermissionColor(permission)
                          )}
                        >
                          {permission}
                        </span>
                      ))}
                    </div>
                  </div>
                  
                  <div className="flex flex-col space-y-2 ml-4">
                    <Button variant="outline" size="sm">
                      <Download className="h-4 w-4 mr-2" />
                      Export
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => regenerateKey(key.id)}>
                      <RefreshCw className="h-4 w-4 mr-2" />
                      Regenerate
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="text-danger hover:text-danger"
                      onClick={() => revokeKey(key.id)}
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      Revoke
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
              <Download className="h-6 w-6" />
              <span>Export All Keys</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Upload className="h-6 w-6" />
              <span>Import Keys</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Shield className="h-6 w-6" />
              <span>Security Audit</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default APIKeys;
