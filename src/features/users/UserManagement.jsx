import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { Users, Plus, Mail, Shield, Eye, Edit, Trash2, Search, Filter, Calendar, Activity, UserPlus, Key, Building, Download } from 'lucide-react';
import { cn } from '../../lib/utils';

const UserManagement = () => {
  const [showInviteForm, setShowInviteForm] = useState(false);
  const [showRoleEditor, setShowRoleEditor] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');

  // Mock users data
  const users = [
    {
      id: 1,
      name: 'Melisa Bayramoğlu',
      email: 'melisa@cyberscope.com',
      role: 'admin',
      status: 'active',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=melisa',
      lastActive: '2024-01-15T14:30:00Z',
      createdAt: '2024-01-01T08:00:00Z',
      permissions: ['read', 'write', 'scan', 'reports', 'admin'],
      twoFactorEnabled: true,
      loginCount: 156,
      lastLogin: '2024-01-15T14:30:00Z',
    },
    {
      id: 2,
      name: 'Ahmet Yılmaz',
      email: 'ahmet@cyberscope.com',
      role: 'analyst',
      status: 'active',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ahmet',
      lastActive: '2024-01-15T12:15:00Z',
      createdAt: '2024-01-05T10:00:00Z',
      permissions: ['read', 'write', 'scan', 'reports'],
      twoFactorEnabled: false,
      loginCount: 89,
      lastLogin: '2024-01-15T12:15:00Z',
    },
    {
      id: 3,
      name: 'Zeynep Kaya',
      email: 'zeynep@cyberscope.com',
      role: 'viewer',
      status: 'active',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=zeynep',
      lastActive: '2024-01-15T09:45:00Z',
      createdAt: '2024-01-08T14:00:00Z',
      permissions: ['read'],
      twoFactorEnabled: true,
      loginCount: 45,
      lastLogin: '2024-01-15T09:45:00Z',
    },
    {
      id: 4,
      name: 'Mehmet Demir',
      email: 'mehmet@cyberscope.com',
      role: 'analyst',
      status: 'inactive',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=mehmet',
      lastActive: '2024-01-10T16:20:00Z',
      createdAt: '2024-01-03T11:00:00Z',
      permissions: ['read', 'write', 'scan'],
      twoFactorEnabled: false,
      loginCount: 67,
      lastLogin: '2024-01-10T16:20:00Z',
    },
    {
      id: 5,
      name: 'Ayşe Özkan',
      email: 'ayse@cyberscope.com',
      role: 'viewer',
      status: 'pending',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ayse',
      lastActive: null,
      createdAt: '2024-01-15T08:00:00Z',
      permissions: ['read'],
      twoFactorEnabled: false,
      loginCount: 0,
      lastLogin: null,
    },
  ];

  const [inviteData, setInviteData] = useState({
    email: '',
    role: 'viewer',
    firstName: '',
    lastName: '',
    message: '',
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

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = roleFilter === 'all' || user.role === roleFilter;
    const matchesStatus = statusFilter === 'all' || user.status === statusFilter;
    
    return matchesSearch && matchesRole && matchesStatus;
  });

  const handleInviteUser = () => {
    if (!inviteData.email || !inviteData.firstName || !inviteData.lastName) {
      alert('Please fill in all required fields');
      return;
    }

    // Mock API call
    console.log('Inviting user:', inviteData);
    
    // Reset form
    setInviteData({
      email: '',
      role: 'viewer',
      firstName: '',
      lastName: '',
      message: '',
    });
    setShowInviteForm(false);
  };

  const handleUserAction = (action, userId) => {
    switch (action) {
      case 'view':
        console.log('Viewing user:', userId);
        break;
      case 'edit':
        console.log('Editing user:', userId);
        break;
      case 'delete':
        if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
          console.log('Deleting user:', userId);
        }
        break;
      case 'suspend':
        if (confirm('Are you sure you want to suspend this user?')) {
          console.log('Suspending user:', userId);
        }
        break;
      case 'activate':
        console.log('Activating user:', userId);
        break;
      default:
        break;
    }
  };

  const InviteUserForm = () => (
    <Card>
      <CardHeader>
        <CardTitle>Invite New User</CardTitle>
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
                onChange={(e) => setInviteData(prev => ({ ...prev, firstName: e.target.value }))}
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-white mb-2">Last Name *</label>
              <Input
                placeholder="Enter last name"
                value={inviteData.lastName}
                onChange={(e) => setInviteData(prev => ({ ...prev, lastName: e.target.value }))}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-white mb-2">Email Address *</label>
            <Input
              type="email"
              placeholder="Enter email address"
              value={inviteData.email}
              onChange={(e) => setInviteData(prev => ({ ...prev, email: e.target.value }))}
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
            <label className="block text-sm font-medium text-white mb-2">Personal Message (Optional)</label>
            <textarea
              placeholder="Add a personal message to the invitation..."
              value={inviteData.message}
              onChange={(e) => setInviteData(prev => ({ ...prev, message: e.target.value }))}
              className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              rows={3}
            />
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3">
            <Button variant="outline" onClick={() => setShowInviteForm(false)}>
              Cancel
            </Button>
            <Button onClick={handleInviteUser}>
              <Mail className="h-4 w-4 mr-2" />
              Send Invitation
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );

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
                
                <div className="flex flex-col space-y-2 ml-4">
                  <Button variant="outline" size="sm">
                    <Edit className="h-4 w-4 mr-2" />
                    Edit
                  </Button>
                  <Button variant="outline" size="sm">
                    <Key className="h-4 w-4 mr-2" />
                    Permissions
                  </Button>
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
            Invite User
          </Button>
        </div>
      </div>

      {/* Invite User Form */}
      {showInviteForm && <InviteUserForm />}

      {/* Role Editor */}
      {showRoleEditor && <RoleEditor />}

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
              <option value="all">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="pending">Pending</option>
              <option value="suspended">Suspended</option>
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
                      <span className={cn(
                        'px-2 py-1 rounded-full text-xs font-medium',
                        getRoleColor(user.role)
                      )}>
                        {roles.find(r => r.id === user.role)?.name || user.role}
                      </span>
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
                          onClick={() => handleUserAction('view', user.id)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUserAction('edit', user.id)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
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
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Download className="h-6 w-6" />
              <span>Export Users</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col space-y-2">
              <Building className="h-6 w-6" />
              <span>Bulk Operations</span>
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

export default UserManagement;
