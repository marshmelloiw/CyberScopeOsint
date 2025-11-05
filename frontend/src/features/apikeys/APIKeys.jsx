import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Key, Plus, Copy, Eye, EyeOff, Trash2, RefreshCw, Download, Upload, Calendar, Globe, Shield, Activity, Loader2, AlertCircle } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const APIKeys = () => {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingKeyId, setEditingKeyId] = useState(null); // ID of key being edited
  const [showSecret, setShowSecret] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const [newKeyData, setNewKeyData] = useState({
    name: '',
    description: '',
    permissions: [],
    expiresAt: '',
    rateLimit: '1000/hour',
    apiKey: '', // Manuel API key (opsiyonel)
    secretKey: '', // Manuel secret key (opsiyonel)
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

  // Fetch API keys from backend
  const fetchApiKeys = useCallback(async (showRefreshing = false) => {
    try {
      if (showRefreshing) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      
      const response = await api.get(endpoints.userApiKeys.list);
      const keysData = response.data.apiKeys || [];
      
      // Transform backend data to frontend format
      const transformedKeys = keysData.map(key => ({
        id: key.id,
        name: key.name,
        key: key.key,
        secret: key.secret,
        status: key.status || 'active',
        permissions: key.permissions || [],
        createdAt: key.createdAt,
        lastUsed: key.lastUsed,
        usageCount: key.usageCount || 0,
        rateLimit: key.rateLimit || '1000/hour',
        expiresAt: key.expiresAt,
        description: key.description || '',
      }));
      
      setApiKeys(transformedKeys);
    } catch (err) {
      console.error('Error fetching API keys:', err);
      setError(err.response?.data?.error || 'API key\'ler yüklenemedi');
      setApiKeys([]);
    } finally {
      if (showRefreshing) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    fetchApiKeys();
  }, [fetchApiKeys]);

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
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
                         (key.description && key.description.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'all' || key.status.toLowerCase() === statusFilter.toLowerCase();
    
    return matchesSearch && matchesStatus;
  });

  const handleCreateKey = async () => {
    if (!newKeyData.name || newKeyData.permissions.length === 0) {
      alert('Lütfen bir isim girin ve en az bir yetki seçin');
      return;
    }

    try {
      // If editing, update; otherwise create
      if (editingKeyId) {
        const requestData = {
          name: newKeyData.name,
          description: newKeyData.description || '',
          permissions: newKeyData.permissions,
          rateLimit: newKeyData.rateLimit,
          expiresAt: newKeyData.expiresAt ? new Date(newKeyData.expiresAt).toISOString() : null,
          // API key ve secret key'i her zaman gönder (boş olsa bile, backend kontrol edecek)
          apiKey: newKeyData.apiKey && newKeyData.apiKey.trim() !== '' ? newKeyData.apiKey.trim() : null,
          secretKey: newKeyData.secretKey && newKeyData.secretKey.trim() !== '' ? newKeyData.secretKey.trim() : null,
        };

        console.log('Updating API key with data:', requestData); // Debug log
        
        await api.put(endpoints.userApiKeys.update(editingKeyId), requestData);
        alert('API key başarıyla güncellendi!');
      } else {
        const requestData = {
          name: newKeyData.name,
          description: newKeyData.description,
          permissions: newKeyData.permissions,
          rateLimit: newKeyData.rateLimit,
          expiresAt: newKeyData.expiresAt ? new Date(newKeyData.expiresAt).toISOString() : null,
          apiKey: newKeyData.apiKey || null, // Manuel API key (opsiyonel)
          secretKey: newKeyData.secretKey || null, // Manuel secret key (opsiyonel)
        };

        await api.post(endpoints.userApiKeys.create, requestData);
        alert('API key başarıyla oluşturuldu!');
      }
    
    // Reset form
    setNewKeyData({
      name: '',
      description: '',
      permissions: [],
      expiresAt: '',
      rateLimit: '1000/hour',
        apiKey: '',
        secretKey: '',
    });
      setEditingKeyId(null);
    setShowCreateForm(false);
      
      // Refresh list
      await fetchApiKeys(false);
    } catch (err) {
      console.error(editingKeyId ? 'Error updating API key:' : 'Error creating API key:', err);
      const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
      alert(editingKeyId ? 'API key güncellenemedi: ' : 'API key oluşturulamadı: ' + errorMsg);
    }
  };

  const handlePermissionToggle = (permission) => {
    setNewKeyData(prev => ({
      ...prev,
      permissions: prev.permissions.includes(permission)
        ? prev.permissions.filter(p => p !== permission)
        : [...prev.permissions, permission]
    }));
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      alert('Panoya kopyalandı!');
    } catch (err) {
      console.error('Failed to copy:', err);
      alert('Kopyalama başarısız');
    }
  };

  const revokeKey = async (id) => {
    if (!window.confirm('Bu API key\'i silmek istediğinize emin misiniz? Bu işlem geri alınamaz.')) {
      return;
    }

    try {
      await api.delete(endpoints.userApiKeys.delete(id));
      alert('API key başarıyla silindi');
      await fetchApiKeys(false);
    } catch (err) {
      console.error('Error deleting API key:', err);
      const errorMsg = err.response?.data?.error || err.message || 'Bilinmeyen hata';
      alert('API key silinemedi: ' + errorMsg);
    }
  };

  const editKey = (key) => {
    console.log('Editing key:', key); // Debug log
    
    // Load all key data into form
    let expiresAtFormatted = '';
    if (key.expiresAt) {
      try {
        // Handle different date formats from backend
        const date = new Date(key.expiresAt);
        if (!isNaN(date.getTime())) {
          // Convert to local datetime format (YYYY-MM-DDTHH:mm)
          const year = date.getFullYear();
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          expiresAtFormatted = `${year}-${month}-${day}T${hours}:${minutes}`;
        }
      } catch (e) {
        console.error('Error parsing expiresAt:', e);
      }
    }

    // Ensure permissions is an array
    const keyPermissions = Array.isArray(key.permissions) ? key.permissions : (key.permissions ? [key.permissions] : []);

    const formData = {
      name: key.name || '',
      description: key.description || '',
      permissions: [...keyPermissions], // Deep copy
      expiresAt: expiresAtFormatted,
      rateLimit: key.rateLimit || '1000/hour',
      apiKey: key.key || '', // Show existing API key
      secretKey: key.secret || '', // Show existing secret key
    };

    console.log('Setting form data:', formData); // Debug log
    
    setNewKeyData(formData);
    setEditingKeyId(key.id);
    setShowCreateForm(true);
  };


  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">API Keys</h1>
          <p className="text-surface-muted">API key'lerinizi ve entegrasyonlarınızı yönetin</p>
        </div>
        <div className="flex items-center space-x-2">
          <Button 
            variant="outline" 
            onClick={() => fetchApiKeys(true)}
            disabled={refreshing || loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
            Yenile
          </Button>
          <Button onClick={() => {
            setEditingKeyId(null);
            setNewKeyData({
              name: '',
              description: '',
              permissions: [],
              expiresAt: '',
              rateLimit: '1000/hour',
              apiKey: '',
              secretKey: '',
            });
            setShowCreateForm(true);
          }}>
            <Plus className="h-4 w-4 mr-2" />
            Yeni API Key
          </Button>
        </div>
      </div>

      {/* Create Key Form */}
      {showCreateForm && (
    <Card>
      <CardHeader>
            <CardTitle>{editingKeyId ? 'API Key Düzenle' : 'Yeni API Key Oluştur'}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
                  <label className="block text-sm font-medium text-white mb-2">Key İsmi *</label>
                  <input
                    type="text"
                    placeholder="örn: Production API Key"
                value={newKeyData.name}
                    onChange={(e) => {
                      setNewKeyData(prev => ({ ...prev, name: e.target.value }));
                    }}
                    className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none"
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
                <label className="block text-sm font-medium text-white mb-2">Açıklama</label>
            <textarea
                  placeholder="Bu API key'in ne için kullanılacağını açıklayın..."
              value={newKeyData.description}
                  onChange={(e) => {
                    setNewKeyData(prev => ({ ...prev, description: e.target.value }));
                  }}
                  className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-y min-h-[80px]"
                  rows={4}
            />
          </div>

              {/* API Key ve Secret Key - Her zaman göster */}
              <div className="border-t border-surface-border pt-4">
                {editingKeyId ? (
                  <p className="text-sm text-surface-muted mb-4">
                    Mevcut API Key ve Secret Key bilgileri. İsterseniz değiştirebilirsiniz.
                  </p>
                ) : (
                  <p className="text-sm text-surface-muted mb-4">
                    Manuel API Key ve Secret Key girmek isterseniz aşağıdaki alanları doldurun. 
                    Boş bırakırsanız otomatik olarak oluşturulacaktır.
                  </p>
                )}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-white mb-2">
                      {editingKeyId ? 'API Key' : 'API Key (Opsiyonel)'}
                    </label>
                    <input
                      type="text"
                      placeholder={editingKeyId ? "API key" : "Manuel API key girin veya boş bırakın"}
                      value={newKeyData.apiKey}
                      onChange={(e) => {
                        setNewKeyData(prev => ({ ...prev, apiKey: e.target.value }));
                      }}
                      className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none font-mono text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-white mb-2">
                      {editingKeyId ? 'Secret Key' : 'Secret Key (Opsiyonel)'}
                    </label>
                    <input
                      type="text"
                      placeholder={editingKeyId ? "Secret key" : "Manuel secret key girin veya boş bırakın"}
                      value={newKeyData.secretKey}
                      onChange={(e) => {
                        setNewKeyData(prev => ({ ...prev, secretKey: e.target.value }));
                      }}
                      className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none font-mono text-sm"
                    />
                  </div>
                </div>
              </div>

          <div>
                <label className="block text-sm font-medium text-white mb-2">Son Kullanma Tarihi (Opsiyonel)</label>
            <Input
              type="datetime-local"
              value={newKeyData.expiresAt}
              onChange={(e) => setNewKeyData(prev => ({ ...prev, expiresAt: e.target.value }))}
            />
          </div>

          {/* Permissions */}
          <div>
                <label className="block text-sm font-medium text-white mb-3">Yetkiler *</label>
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
                          onChange={() => {}}
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
                <Button variant="outline" onClick={() => {
                  setShowCreateForm(false);
                  setEditingKeyId(null);
                  setNewKeyData({
                    name: '',
                    description: '',
                    permissions: [],
                    expiresAt: '',
                    rateLimit: '1000/hour',
                    apiKey: '',
                    secretKey: '',
                  });
                }}>
                  İptal
            </Button>
            <Button onClick={handleCreateKey}>
                  {editingKeyId ? (
                    <>
                      <RefreshCw className="h-4 w-4 mr-2" />
                      Kaydet
                    </>
                  ) : (
                    <>
              <Plus className="h-4 w-4 mr-2" />
                      API Key Oluştur
                    </>
                  )}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
      )}

      {/* Filters and search */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Key className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-muted" />
                <Input
                  placeholder="API key'leri isim veya açıklamaya göre ara..."
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
              <option value="all">Tüm Durumlar</option>
              <option value="active">Aktif</option>
              <option value="inactive">Pasif</option>
              <option value="expired">Süresi Dolmuş</option>
              <option value="suspended">Askıya Alınmış</option>
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
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
              <span className="ml-3 text-surface-muted">API key'ler yükleniyor...</span>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <AlertCircle className="h-16 w-16 mx-auto mb-4 text-danger" />
                <p className="text-danger mb-2">{error}</p>
                <Button onClick={() => fetchApiKeys(false)} variant="outline">
                  Tekrar Dene
                </Button>
              </div>
            </div>
          ) : filteredKeys.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-surface-muted mb-4">
                {apiKeys.length === 0 
                  ? 'Henüz API key yok. Yeni bir API key oluşturun.'
                  : 'Arama kriterlerinize uygun API key bulunamadı.'}
              </p>
              {apiKeys.length === 0 && (
                <Button onClick={() => {
                  setEditingKeyId(null);
                  setNewKeyData({
                    name: '',
                    description: '',
                    permissions: [],
                    expiresAt: '',
                    rateLimit: '1000/hour',
                    apiKey: '',
                    secretKey: '',
                  });
                  setShowCreateForm(true);
                }}>
                  <Plus className="h-4 w-4 mr-2" />
                  İlk API Key'i Oluştur
                </Button>
              )}
            </div>
          ) : (
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
                    
                      {key.description && (
                    <p className="text-surface-muted mb-3">{key.description}</p>
                      )}
                    
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-surface-muted">API Key:</span>
                        <div className="flex items-center space-x-2 mt-1">
                            <span className="font-mono text-white text-xs">{key.key}</span>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => copyToClipboard(key.key)}
                              title="Kopyala"
                          >
                            <Copy className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                      
                      <div>
                        <span className="text-surface-muted">Secret:</span>
                        <div className="flex items-center space-x-2 mt-1">
                            <span className="font-mono text-white text-xs">
                            {showSecret[key.id] ? key.secret : '••••••••••••••••••••'}
                          </span>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setShowSecret(prev => ({ ...prev, [key.id]: !prev[key.id] }))}
                              title={showSecret[key.id] ? 'Gizle' : 'Göster'}
                          >
                            {showSecret[key.id] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => copyToClipboard(key.secret)}
                              title="Kopyala"
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
                          <span className="text-surface-muted">Kullanım:</span>
                        <span className="text-white ml-2">{key.usageCount.toLocaleString()}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4 mt-3 text-sm text-surface-muted">
                        {key.createdAt && (
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                            <span>Oluşturuldu: {new Date(key.createdAt).toLocaleDateString('tr-TR')}</span>
                      </div>
                        )}
                        {key.lastUsed && (
                      <div className="flex items-center space-x-1">
                        <Activity className="h-4 w-4" />
                            <span>Son Kullanım: {new Date(key.lastUsed).toLocaleDateString('tr-TR')}</span>
                      </div>
                        )}
                      {key.expiresAt && (
                        <div className="flex items-center space-x-1">
                          <Globe className="h-4 w-4" />
                            <span>Bitiş: {key.expiresAt.includes('T') ? new Date(key.expiresAt).toLocaleDateString('tr-TR') : new Date(key.expiresAt + 'T00:00:00').toLocaleDateString('tr-TR')}</span>
                        </div>
                      )}
                    </div>
                    
                      {key.permissions && key.permissions.length > 0 && (
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
                      )}
                  </div>
                  
                  <div className="flex flex-col space-y-2 ml-4">
                      <Button variant="outline" size="sm" onClick={() => editKey(key)}>
                      <RefreshCw className="h-4 w-4 mr-2" />
                        Düzenle
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="text-danger hover:text-danger"
                      onClick={() => revokeKey(key.id)}
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                        Sil
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

export default APIKeys;
