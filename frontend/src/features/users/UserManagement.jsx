import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Users, Plus, Mail, Shield, Edit, Trash2, Search, Filter, Calendar, Activity, UserPlus, Key, Building, Download, Loader2, AlertCircle, FileText, X, Eye } from 'lucide-react';
import { cn } from '../../lib/utils';
import api, { endpoints } from '../../lib/axios';

const UserManagement = () => {
  const [showInviteForm, setShowInviteForm] = useState(false);
  const [showRoleEditor, setShowRoleEditor] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [editingUserId, setEditingUserId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('active');
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [roleUpdateLoading, setRoleUpdateLoading] = useState({});
  const [inviteData, setInviteData] = useState({
    email: '',
    role: 'viewer',
    firstName: '',
    lastName: '',
    password: '',
    phoneNumber: '',
    userFile: null,
    selectedFile: null, // For file input display
  });

  const roles = [
    { id: 'admin', name: 'Administrator', description: 'Full system access', color: 'bg-danger/20 text-danger' },
    { id: 'analyst', name: 'Security Analyst', description: 'Scan, analyze, and report', color: 'bg-warning/20 text-warning' },
    { id: 'viewer', name: 'Viewer', description: 'Read-only access', color: 'bg-info/20 text-info' },
  ];

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-success/20 text-success border-success/30';
      case 'inactive': return 'bg-surface-muted/20 text-surface-muted border-surface-muted/30';
      case 'pending': return 'bg-warning/20 text-warning border-warning/30';
      case 'suspended': return 'bg-danger/20 text-danger border-danger/30';
      default: return 'bg-surface-muted/20 text-surface-muted border-surface-muted/30';
    }
  };

  const getRoleColor = (role) => {
    const roleData = roles.find(r => r.id === role);
    return roleData ? roleData.color : 'bg-surface-muted/20 text-surface-muted';
  };

  const [editData, setEditData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    role: 'viewer',
    status: 'active',
    phoneNumber: '',
    userFile: null,
    selectedFile: null, // For file input display
    deleteFile: false, // Flag to delete existing file
    mfaEnabled: false,
  });

  // Fetch users from backend
  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get(endpoints.users.list);
      const usersData = response.data.users || [];
      setUsers(usersData);
    } catch (err) {
      console.error('Error fetching users:', err);
      setError(err.response?.data?.error || 'Users could not be loaded');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const filteredUsers = users.filter(user => {
    const matchesSearch = (user.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.email || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = roleFilter === 'all' || user.role === roleFilter;
    const matchesStatus = user.status === statusFilter;

    return matchesSearch && matchesRole && matchesStatus;
  });

  const handleFileUpload = async (file) => {
    if (!file) return null;

    // Validate file type
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      alert('Only PDF files can be uploaded');
      return null;
    }

    try {
      const formData = new FormData();
      formData.append('file', file);

      // Axios will automatically set Content-Type with boundary for FormData
      const response = await api.post('/users/upload-file', formData);

      return response.data.filePath;
    } catch (err) {
      console.error('Error uploading file:', err);
      alert('Error occurred while uploading file: ' + (err.response?.data?.error || err.message));
      return null;
    }
  };

  const handleInviteUser = async () => {
    if (!inviteData.email || !inviteData.firstName || !inviteData.lastName || !inviteData.password) {
      alert('Please fill in all required fields (Email, First Name, Last Name, Password)');
      return;
    }

    // Password validation
    if (inviteData.password.length < 6) {
      alert('Password must be at least 6 characters');
      return;
    }

    try {
      // Upload file if selected
      let filePath = null;
      if (inviteData.selectedFile) {
        filePath = await handleFileUpload(inviteData.selectedFile);
        if (!filePath) {
          return; // Upload failed, stop here
        }
      }

      await api.post(endpoints.users.create, {
        email: inviteData.email,
        firstName: inviteData.firstName,
        lastName: inviteData.lastName,
        role: inviteData.role,
        password: inviteData.password,
        phoneNumber: inviteData.phoneNumber || null,
        userFile: filePath,
      });

      alert('User successfully created! User can login with the specified email and password.');

      // Reset form
      setInviteData({
        email: '',
        role: 'viewer',
        firstName: '',
        lastName: '',
        password: '',
        phoneNumber: '',
        userFile: null,
        selectedFile: null,
      });
      setShowInviteForm(false);

      // Refresh users list
      await fetchUsers();
    } catch (err) {
      console.error('Error creating user:', err);
      alert('User could not be created: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleEditUser = useCallback((user) => {
    console.log('handleEditUser called with user object:', user);
    console.log('user.phoneNumber value:', user.phoneNumber);
    // Parse name into firstName and lastName
    const nameParts = (user.name || '').split(' ');
    const firstName = nameParts[0] || '';
    const lastName = nameParts.slice(1).join(' ') || '';

    const editDataNew = {
      email: user.email || '',
      firstName: firstName,
      lastName: lastName,
      role: user.role || 'viewer',
      status: user.status || 'active',
      phoneNumber: user.phoneNumber || '',
      userFile: user.userFile || null,
      selectedFile: null,
      deleteFile: false,
      mfaEnabled: user.twoFactorEnabled || false,
    };

    console.log('Setting edit data:', editDataNew);
    console.log('editDataNew.phoneNumber:', editDataNew.phoneNumber);
    console.log('editDataNew.userFile:', editDataNew.userFile);
    console.log('user object from backend:', user);
    setEditData(editDataNew);
    setEditingUserId(user.id);
    setShowEditForm(true);
  }, []);

  const handleUpdateUser = async () => {
    if (!editingUserId) {
      alert('User to edit not found');
      return;
    }

    if (!editData.email || !editData.firstName || !editData.lastName) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      console.log('Updating user:', editingUserId, 'with data:', editData);
      console.log('Phone number being sent:', editData.phoneNumber);

      // Handle file operations
      let filePath = editData.userFile; // Keep existing file path by default

      // If user wants to delete file
      if (editData.deleteFile) {
        filePath = null; // Set to null to delete file
      }
      // If new file is selected, upload it
      else if (editData.selectedFile) {
        filePath = await handleFileUpload(editData.selectedFile);
        if (!filePath) {
          return; // Upload failed, stop here
        }
      }

      const response = await api.put(endpoints.users.update(editingUserId), {
        email: editData.email,
        firstName: editData.firstName,
        lastName: editData.lastName,
        role: editData.role,
        status: editData.status,
        phoneNumber: editData.phoneNumber || null,
        userFile: filePath, // Can be null to delete, existing path, or new path
        mfaEnabled: editData.mfaEnabled,
      });

      console.log('Update response:', response.data);
      console.log('Updated phone number:', response.data.phoneNumber);
      alert('User successfully updated!');

      // Reset form
      setShowEditForm(false);
      setEditingUserId(null);
      setEditData({
        email: '',
        firstName: '',
        lastName: '',
        role: 'viewer',
        status: 'active',
        phoneNumber: '',
        userFile: null,
        selectedFile: null,
        deleteFile: false,
        mfaEnabled: false,
      });

      // Refresh users list
      await fetchUsers();
    } catch (err) {
      console.error('Error updating user:', err);
      const errorMsg = err.response?.data?.error || err.message || 'Unknown error';
      alert('User could not be updated: ' + errorMsg);
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
      return;
    }

    try {
      await api.delete(endpoints.users.delete(userId));
      alert('User successfully deleted!');
      await fetchUsers();
    } catch (err) {
      console.error('Error deleting user:', err);
      alert('User could not be deleted: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleSuspendUser = async (userId) => {
    if (!window.confirm('Are you sure you want to suspend this user?')) {
      return;
    }

    try {
      await api.put(`/users/${userId}/suspend`);
      alert('User successfully suspended!');
      await fetchUsers();
    } catch (err) {
      console.error('Error suspending user:', err);
      alert('User could not be suspended: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleActivateUser = async (userId) => {
    try {
      await api.put(`/users/${userId}/activate`);
      alert('User successfully activated!');
      await fetchUsers();
    } catch (err) {
      console.error('Error activating user:', err);
      alert('User could not be activated: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleExportUsers = async (format = 'csv') => {
    try {
      const response = await api.get(`/users/export?format=${format}`, {
        responseType: format === 'csv' ? 'blob' : 'json',
      });

      if (format === 'csv') {
        // Download CSV file
        const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'users_export.csv');
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
        alert('Users successfully exported in CSV format!');
      } else {
        // Download JSON file
        const dataStr = JSON.stringify(response.data, null, 2);
        const blob = new Blob([dataStr], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'users_export.json');
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
        alert('Users successfully exported in JSON format!');
      }
    } catch (err) {
      console.error('Error exporting users:', err);
      alert('Export operation failed: ' + (err.response?.data?.error || err.message));
    }
  };

  const [showSecurityAudit, setShowSecurityAudit] = useState(false);
  const [securityAuditData, setSecurityAuditData] = useState(null);

  const handleSecurityAudit = async () => {
    try {
      const response = await api.get('/users/security-audit');
      setSecurityAuditData(response.data);
      setShowSecurityAudit(true);
    } catch (err) {
      console.error('Error fetching security audit:', err);
      alert('Security audit report could not be retrieved: ' + (err.response?.data?.error || err.message));
    }
  };

  const [showBulkOperations, setShowBulkOperations] = useState(false);
  const [selectedUsers, setSelectedUsers] = useState([]);

  const handleBulkOperations = () => {
    setShowBulkOperations(true);
  };

  const handleBulkAction = async (action) => {
    if (selectedUsers.length === 0) {
      alert('Please select at least one user');
      return;
    }

    if (!window.confirm(`Are you sure you want to perform ${action} on ${selectedUsers.length} users?`)) {
      return;
    }

    try {
      const promises = selectedUsers.map(userId => {
        switch (action) {
          case 'activate':
            return api.put(`/users/${userId}/activate`);
          case 'suspend':
            return api.put(`/users/${userId}/suspend`);
          case 'delete':
            return api.delete(`/users/${userId}`);
          default:
            return Promise.resolve();
        }
      });

      await Promise.all(promises);
      alert(`${action} operation successfully completed on ${selectedUsers.length} users!`);
      setSelectedUsers([]);
      setShowBulkOperations(false);
      await fetchUsers();
    } catch (err) {
      console.error('Error performing bulk action:', err);
      alert('Bulk operation failed: ' + (err.response?.data?.error || err.message));
    }
  };

  const handleUserAction = useCallback((action, userId) => {
    switch (action) {
      case 'edit':
        const user = users.find(u => u.id === userId);
        if (user) {
          console.log('Editing user:', user);
          handleEditUser(user);
        } else {
          console.error('User not found:', userId);
          alert('User not found');
        }
        break;
      case 'delete':
        handleDeleteUser(userId);
        break;
      case 'suspend':
        handleSuspendUser(userId);
        break;
      case 'activate':
        handleActivateUser(userId);
        break;
      default:
        break;
    }
  }, [users]);

  const handleRoleChange = async (userId, newRole) => {
    if (!newRole) return;

    setRoleUpdateLoading((prev) => ({ ...prev, [userId]: true }));
    try {
      await api.put(endpoints.users.update(userId), { role: newRole });
      setUsers((prevUsers) => prevUsers.map((user) => (user.id === userId ? { ...user, role: newRole } : user)));
    } catch (err) {
      console.error('Error updating role:', err);
      alert('Role could not be updated: ' + (err.response?.data?.error || err.message));
    } finally {
      setRoleUpdateLoading((prev) => {
        const next = { ...prev };
        delete next[userId];
        return next;
      });
    }
  };

  const InviteUserForm = useMemo(() => (
    <Card>
      <CardHeader>
        <CardTitle>Create New User</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white mb-2">First Name *</label>
              <Input
                placeholder="Enter first name"
                value={inviteData.firstName}
                onChange={(e) => {
                  const value = e.target.value;
                  setInviteData(prev => ({ ...prev, firstName: value }));
                }}
                style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-white mb-2">Last Name *</label>
              <Input
                placeholder="Enter last name"
                value={inviteData.lastName}
                onChange={(e) => {
                  const value = e.target.value;
                  setInviteData(prev => ({ ...prev, lastName: value }));
                }}
                style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Email Address *</label>
            <Input
              type="email"
              placeholder="Enter email address"
              value={inviteData.email}
              onChange={(e) => {
                const value = e.target.value;
                setInviteData(prev => ({ ...prev, email: value }));
              }}
              style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Password *</label>
            <Input
              type="password"
              placeholder="Enter password (at least 6 characters)"
              value={inviteData.password}
              onChange={(e) => {
                const value = e.target.value;
                setInviteData(prev => ({ ...prev, password: value }));
              }}
              style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
            />
            <p className="text-xs text-surface-muted mt-1">User can login with this email and password</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Phone Number</label>
            <Input
              type="tel"
              placeholder="Enter phone number (e.g. +905551234567)"
              value={inviteData.phoneNumber}
              onChange={(e) => {
                const value = e.target.value;
                setInviteData(prev => ({ ...prev, phoneNumber: value }));
              }}
              style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Role</label>
            <select
              value={inviteData.role}
              onChange={(e) => setInviteData(prev => ({ ...prev, role: e.target.value }))}
              className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {roles.map((role) => (
                <option key={role.id} value={role.id}>{role.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Upload File (PDF)</label>
            <div className="space-y-2">
              <input
                type="file"
                accept=".pdf"
                onChange={(e) => {
                  const file = e.target.files?.[0] || null;
                  setInviteData(prev => ({ ...prev, selectedFile: file }));
                }}
                className="w-full text-sm text-white
                  file:mr-4 file:py-2 file:px-4
                  file:rounded-lg file:border-0
                  file:text-sm file:font-semibold
                  file:bg-primary-600 file:text-white
                  hover:file:bg-primary-700
                  file:cursor-pointer
                  bg-surface-panel border border-surface-border rounded-lg
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
              {inviteData.selectedFile && (
                <div className="flex items-center justify-between p-2 bg-surface-panel border border-surface-border rounded-lg">
                  <div className="flex items-center space-x-2">
                    <FileText className="h-4 w-4 text-primary" />
                    <span className="text-sm text-white">{inviteData.selectedFile.name}</span>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setInviteData(prev => ({ ...prev, selectedFile: null }))}
                    className="text-danger hover:text-danger"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              )}
            </div>
            <p className="text-xs text-surface-muted mt-1">Only PDF files can be uploaded</p>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowInviteForm(false)}>
              Cancel
            </Button>
            <Button onClick={handleInviteUser}>
              <Mail className="h-4 w-4 mr-2" />
              Create User
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  ), [inviteData, roles, handleInviteUser]);

  const EditUserForm = useMemo(() => (
    <Card>
      <CardHeader>
        <CardTitle>Edit User</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-white mb-2">First Name *</label>
              <Input
                placeholder="Enter first name"
                value={editData.firstName}
                onChange={(e) => {
                  const value = e.target.value;
                  setEditData(prev => ({ ...prev, firstName: value }));
                }}
                style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-white mb-2">Last Name *</label>
              <Input
                placeholder="Enter last name"
                value={editData.lastName}
                onChange={(e) => {
                  const value = e.target.value;
                  setEditData(prev => ({ ...prev, lastName: value }));
                }}
                style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Email *</label>
            <Input
              type="email"
              placeholder="Email address"
              value={editData.email}
              onChange={(e) => {
                const value = e.target.value;
                setEditData(prev => ({ ...prev, email: value }));
              }}
              style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Role</label>
            <select
              value={editData.role}
              onChange={(e) => setEditData(prev => ({ ...prev, role: e.target.value }))}
              className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              {roles.map((role) => (
                <option key={role.id} value={role.id}>{role.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Status</label>
            <select
              value={editData.status}
              onChange={(e) => setEditData(prev => ({ ...prev, status: e.target.value }))}
              className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Phone Number</label>
            <Input
              type="tel"
              placeholder="Enter phone number (e.g. +905551234567)"
              value={editData.phoneNumber}
              onChange={(e) => {
                const value = e.target.value;
                setEditData(prev => ({ ...prev, phoneNumber: value }));
              }}
              style={{ fontFamily: 'inherit', fontSize: 'inherit' }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Upload File (PDF)</label>
            <div className="space-y-2">
              {editData.userFile && !editData.selectedFile && (
                <div className="flex items-center justify-between p-2 bg-surface-panel border border-surface-border rounded-lg mb-2">
                  <div className="flex items-center space-x-2">
                    <FileText className="h-4 w-4 text-primary" />
                    <span className="text-sm text-white" title={editData.userFile}>
                      {editData.userFile.includes('/') ? editData.userFile.split('/').pop() : editData.userFile}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        if (window.confirm('Are you sure you want to delete the existing file?')) {
                          setEditData(prev => ({ ...prev, userFile: null, deleteFile: true }));
                        }
                      }}
                      className="text-danger hover:text-danger"
                      title="Delete File"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                    <span className="text-xs text-surface-muted">Existing file</span>
                  </div>
                </div>
              )}
              <input
                type="file"
                accept=".pdf"
                onChange={(e) => {
                  const file = e.target.files?.[0] || null;
                  setEditData(prev => ({ ...prev, selectedFile: file }));
                }}
                className="w-full text-sm text-white
                  file:mr-4 file:py-2 file:px-4
                  file:rounded-lg file:border-0
                  file:text-sm file:font-semibold
                  file:bg-primary-600 file:text-white
                  hover:file:bg-primary-700
                  file:cursor-pointer
                  bg-surface-panel border border-surface-border rounded-lg
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
              {editData.selectedFile && (
                <div className="flex items-center justify-between p-2 bg-surface-panel border border-surface-border rounded-lg">
                  <div className="flex items-center space-x-2">
                    <FileText className="h-4 w-4 text-primary" />
                    <span className="text-sm text-white">{editData.selectedFile.name}</span>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setEditData(prev => ({ ...prev, selectedFile: null }))}
                    className="text-danger hover:text-danger"
                    title="Cancel file selection"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              )}
            </div>
            <p className="text-xs text-surface-muted mt-1">
              {editData.userFile
                ? 'If you select a new file, the existing file will be replaced, or click delete to remove it'
                : 'Select PDF file'}
            </p>
          </div>

          <div>
            <label className="flex items-center space-x-2">
              <input
                type="checkbox"
                checked={editData.mfaEnabled}
                onChange={(e) => setEditData(prev => ({ ...prev, mfaEnabled: e.target.checked }))}
                className="rounded border-surface-border bg-surface-panel text-primary-600"
              />
              <span className="text-sm text-white">Two-Factor Authentication (2FA)</span>
            </label>
          </div>

          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => {
              setShowEditForm(false);
              setEditingUserId(null);
            }}>
              Cancel
            </Button>
            <Button onClick={handleUpdateUser}>
              <Edit className="h-4 w-4 mr-2" />
              Save
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  ), [editData, roles, handleUpdateUser, setShowEditForm, setEditingUserId]);

  const RoleEditor = () => (
    <Card>
      <CardHeader>
        <CardTitle>Role Management</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          {roles.map((role) => (
            <div key={role.id} className="p-4 border border-surface-border rounded-lg">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3 mb-2">
                    <Shield className="h-5 w-5 text-primary" />
                    <h4 className="text-lg font-semibold text-white">{role.name}</h4>
                    <span className={cn('px-2 py-1 rounded-full text-xs font-medium', role.color)}>
                      {role.id.toUpperCase()}
                    </span>
                  </div>

                  <p className="text-surface-muted mb-3">{role.description}</p>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <h5 className="font-medium text-white mb-2">Permissions</h5>
                      <div className="space-y-2">
                        {role.id === 'admin' && (
                          <>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Full System Access</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">User Management</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">System Configuration</span>
                            </div>
                          </>
                        )}
                        {role.id === 'analyst' && (
                          <>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Read Access</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Write Access</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Scan Operations</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Report Generation</span>
                            </div>
                          </>
                        )}
                        {role.id === 'viewer' && (
                          <>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">Read Access</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <input type="checkbox" defaultChecked disabled className="rounded border-surface-border bg-surface-panel text-primary-600" />
                              <span className="text-sm text-white">View Reports</span>
                            </div>
                          </>
                        )}
                      </div>
                    </div>

                    <div>
                      <h5 className="font-medium text-white mb-2">Role Statistics</h5>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-surface-muted">Active Users:</span>
                          <span className="text-white">
                            {users.filter(u => u.role === role.id && u.status === 'active').length}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-surface-muted">Total Users:</span>
                          <span className="text-white">
                            {users.filter(u => u.role === role.id).length}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">User Management</h1>
          <p className="text-surface-muted">Manage users, roles, and permissions</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button variant="outline" onClick={() => setShowRoleEditor(!showRoleEditor)}>
            <Shield className="h-4 w-4 mr-2" />
            Roles
          </Button>
          <Button onClick={() => setShowInviteForm(true)}>
            <UserPlus className="h-4 w-4 mr-2" />
            Create User
          </Button>
        </div>
      </div>

      {/* Invite User Form */}
      {showInviteForm && InviteUserForm}

      {/* Edit User Form */}
      {showEditForm && EditUserForm}

      {/* Role Editor */}
      {showRoleEditor && <RoleEditor />}

      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center h-96">
          <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
          <span className="ml-3 text-surface-muted">Loading users...</span>
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <Card>
          <CardContent className="p-12 text-center">
            <AlertCircle className="h-16 w-16 mx-auto mb-4 text-danger" />
            <p className="text-danger mb-2">{error}</p>
            <Button onClick={fetchUsers} variant="outline">
              Try Again
            </Button>
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
                <Users className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-muted" />
                <Input
                  placeholder="Search users by name or email..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            {/* Role filter */}
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="all">All Roles</option>
              {roles.map((role) => (
                <option key={role.id} value={role.id}>{role.name}</option>
              ))}
            </select>

            {/* Status filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Users table */}
      <Card>
        <CardHeader>
          <CardTitle>Users ({filteredUsers.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-surface-border">
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">User</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Role</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Status</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Last Active</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">2FA</th>
                  <th className="text-left p-3 text-sm font-medium text-surface-muted">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id} className="border-b border-surface-border/50 hover:bg-surface-panel/50">
                    <td className="p-3">
                      <div className="flex items-center space-x-3">
                        <img
                          src={user.avatar}
                          alt={user.name}
                          className="h-10 w-10 rounded-full"
                        />
                        <div>
                          <p className="font-medium text-white">{user.name}</p>
                          <p className="text-sm text-surface-muted">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="space-y-2">
                        <span
                          className={cn(
                            'px-2 py-1 rounded-full text-xs font-medium',
                            getRoleColor(user.role)
                          )}
                        >
                          {roles.find(r => r.id === user.role)?.name || user.role}
                        </span>
                        <div className="flex items-center space-x-2">
                          <select
                            value={user.role}
                            onChange={(e) => handleRoleChange(user.id, e.target.value)}
                            disabled={!!roleUpdateLoading[user.id]}
                            className="w-full px-3 py-1 text-xs bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:opacity-60"
                          >
                            {roles.map((roleOption) => (
                              <option key={roleOption.id} value={roleOption.id}>
                                {roleOption.name}
                              </option>
                            ))}
                          </select>
                          {roleUpdateLoading[user.id] && <Loader2 className="h-4 w-4 animate-spin text-primary-500" />}
                        </div>
                      </div>
                    </td>
                    <td className="p-3">
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs font-medium border',
                        getStatusColor(user.status)
                      )}>
                        {user.status.toUpperCase()}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="text-sm">
                        {user.lastActive ? (
                          <div className="flex items-center space-x-1">
                            <Activity className="h-4 w-4 text-surface-muted" />
                            <span className="text-white">
                              {new Date(user.lastActive).toLocaleDateString()}
                            </span>
                          </div>
                        ) : (
                          <span className="text-surface-muted">Never</span>
                        )}
                      </div>
                    </td>
                    <td className="p-3">
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs font-medium',
                        user.twoFactorEnabled
                          ? 'bg-success/20 text-success'
                          : 'bg-surface-muted/20 text-surface-muted'
                      )}>
                        {user.twoFactorEnabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center space-x-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUserAction('edit', user.id)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        {user.userFile && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
                              const fileUrl = `${baseURL}/users/user-file/${user.id}`;
                              window.open(fileUrl, '_blank');
                            }}
                            className="text-primary hover:text-primary"
                            title="View File"
                          >
                            <FileText className="h-4 w-4" />
                          </Button>
                        )}
                        {user.status === 'active' ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleUserAction('suspend', user.id)}
                            className="text-warning hover:text-warning"
                          >
                            <Shield className="h-4 w-4" />
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleUserAction('activate', user.id)}
                            className="text-success hover:text-success"
                          >
                            <Activity className="h-4 w-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUserAction('delete', user.id)}
                          className="text-danger hover:text-danger"
                        >
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

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Button
              variant="outline"
              className="h-20 flex-col space-y-2"
              onClick={() => handleExportUsers('csv')}
            >
              <Download className="h-6 w-6" />
              <span>Export Users</span>
            </Button>
            <Button
              variant="outline"
              className="h-20 flex-col space-y-2"
              onClick={handleBulkOperations}
            >
              <Building className="h-6 w-6" />
              <span>Bulk Operations</span>
            </Button>
            <Button
              variant="outline"
              className="h-20 flex-col space-y-2"
              onClick={handleSecurityAudit}
            >
              <Shield className="h-6 w-6" />
              <span>Security Audit</span>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Bulk Operations Modal */}
      {showBulkOperations && (
        <Card>
          <CardHeader>
            <CardTitle>Bulk Operations</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <p className="text-surface-muted">Select users for bulk operation:</p>

              <div className="max-h-60 overflow-y-auto border border-surface-border rounded-lg p-4">
                {users.map(user => (
                  <label key={user.id} className="flex items-center space-x-2 p-2 hover:bg-surface-panel rounded">
                    <input
                      type="checkbox"
                      checked={selectedUsers.includes(user.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedUsers([...selectedUsers, user.id]);
                        } else {
                          setSelectedUsers(selectedUsers.filter(id => id !== user.id));
                        }
                      }}
                      className="rounded border-surface-border bg-surface-panel text-primary-600"
                    />
                    <span className="text-white">{user.name} ({user.email})</span>
                  </label>
                ))}
              </div>

              <div className="flex items-center justify-between">
                <span className="text-surface-muted">
                  {selectedUsers.length} users selected
                </span>
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    onClick={() => {
                      setShowBulkOperations(false);
                      setSelectedUsers([]);
                    }}
                  >
                    Cancel
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => handleBulkAction('activate')}
                    disabled={selectedUsers.length === 0}
                  >
                    Activate
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => handleBulkAction('suspend')}
                    disabled={selectedUsers.length === 0}
                  >
                    Suspend
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => handleBulkAction('delete')}
                    disabled={selectedUsers.length === 0}
                    className="text-danger hover:text-danger"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Security Audit Modal */}
      {showSecurityAudit && securityAuditData && (
        <Card>
          <CardHeader>
            <CardTitle>Security Audit Report</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted">Total Users</p>
                  <p className="text-2xl font-bold text-white">{securityAuditData.totalUsers}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted">Active Users</p>
                  <p className="text-2xl font-bold text-success">{securityAuditData.activeUsers}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted">Inactive Users</p>
                  <p className="text-2xl font-bold text-danger">{securityAuditData.inactiveUsers}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted">Users with 2FA</p>
                  <p className="text-2xl font-bold text-white">{securityAuditData.usersWithMfa}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted mb-2">Users Never Logged In</p>
                  <p className="text-xl font-semibold text-warning">{securityAuditData.usersNeverLoggedIn}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted mb-2">Users Inactive 30+ Days</p>
                  <p className="text-xl font-semibold text-warning">{securityAuditData.usersInactive30Days}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted mb-2">Users Active Last 7 Days</p>
                  <p className="text-xl font-semibold text-success">{securityAuditData.usersActive7Days}</p>
                </div>
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted mb-2">Users Without 2FA</p>
                  <p className="text-xl font-semibold text-danger">{securityAuditData.usersWithoutMfa}</p>
                </div>
              </div>

              {securityAuditData.roleDistribution && (
                <div className="p-4 bg-surface-panel rounded-lg">
                  <p className="text-sm text-surface-muted mb-2">Role Distribution</p>
                  <div className="space-y-1">
                    {Object.entries(securityAuditData.roleDistribution).map(([role, count]) => (
                      <div key={role} className="flex justify-between">
                        <span className="text-white capitalize">{role}</span>
                        <span className="text-surface-muted">{count}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {securityAuditData.recommendations && securityAuditData.recommendations.length > 0 && (
                <div className="p-4 bg-warning/20 border border-warning/30 rounded-lg">
                  <p className="text-sm font-semibold text-warning mb-2">Security Recommendations</p>
                  <ul className="list-disc list-inside space-y-1 text-sm text-warning">
                    {securityAuditData.recommendations.map((rec, idx) => (
                      <li key={idx}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="flex justify-end">
                <Button variant="outline" onClick={() => setShowSecurityAudit(false)}>
                  Close
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default UserManagement;
