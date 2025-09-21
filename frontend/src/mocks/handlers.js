import { http, HttpResponse } from 'msw';

// Mock data
const mockUsers = [
  {
    id: 1,
    name: 'Melisa Bayramoğlu',
    email: 'melisa@cyberscope.com',
    role: 'admin',
    status: 'active',
    lastActive: '2024-01-15T10:30:00Z',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=melisa',
  },
  {
    id: 2,
    name: 'Gül Yasemin',
    email: 'gul@cyberscope.com',
    role: 'analyst',
    status: 'active',
    lastActive: '2024-01-15T09:15:00Z',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=gul',
  },
  {
    id: 3,
    name: 'Nuri Duldar',
    email: 'nuri@cyberscope.com',
    role: 'analyst',
    status: 'active',
    lastActive: '2024-01-15T08:45:00Z',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=nuri',
  },
  {
    id: 4,
    name: 'Damla Düzgün',
    email: 'damla@cyberscope.com',
    role: 'viewer',
    status: 'active',
    lastActive: '2024-01-15T07:30:00Z',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=damla',
  },
];

const mockScans = [
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
];

const mockNotifications = [
  {
    id: 1,
    title: 'High Risk Finding Detected',
    message: 'Critical vulnerability found in domain scan',
    type: 'alert',
    severity: 'high',
    read: false,
    createdAt: '2024-01-15T10:00:00Z',
    link: '/scans/1',
  },
  {
    id: 2,
    title: 'Scan Completed',
    message: 'Domain security assessment finished',
    type: 'info',
    severity: 'low',
    read: false,
    createdAt: '2024-01-15T08:05:00Z',
    link: '/scans/1',
  },
  {
    id: 3,
    title: 'New Breach Detected',
    message: 'Email address found in recent data breach',
    type: 'warning',
    severity: 'medium',
    read: true,
    createdAt: '2024-01-15T07:02:00Z',
    link: '/scans/2',
  },
];

const mockApiKeys = [
  {
    id: 1,
    name: 'Production API Key',
    key: 'csk_prod_1234567890abcdef',
    scopes: ['read', 'write'],
    lastUsed: '2024-01-15T09:30:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    status: 'active',
  },
  {
    id: 2,
    name: 'Development API Key',
    key: 'csk_dev_abcdef1234567890',
    scopes: ['read'],
    lastUsed: '2024-01-14T15:20:00Z',
    createdAt: '2024-01-10T00:00:00Z',
    status: 'active',
  },
];

const mockReports = [
  {
    id: 1,
    title: 'Monthly Security Report - January 2024',
    type: 'monthly',
    status: 'completed',
    createdAt: '2024-01-15T00:00:00Z',
    scans: [1, 2, 3],
    risk: 4.5,
    findings: 15,
  },
  {
    id: 2,
    title: 'Domain Security Assessment Report',
    type: 'scan',
    status: 'completed',
    createdAt: '2024-01-15T08:05:00Z',
    scans: [1],
    risk: 3,
    findings: 12,
  },
];

// Mock handlers
export const handlers = [
  // Authentication
  http.post('/api/auth/login', () => {
    return HttpResponse.json({
      token: 'mock_jwt_token_12345',
      refreshToken: 'mock_refresh_token_12345',
      user: mockUsers[0],
    });
  }),

  http.post('/api/auth/register', () => {
    return HttpResponse.json({
      message: 'User registered successfully',
      user: mockUsers[0],
    });
  }),

  http.post('/api/auth/mfa/verify', () => {
    return HttpResponse.json({
      token: 'mock_jwt_token_12345',
    });
  }),

  http.post('/api/auth/refresh', () => {
    return HttpResponse.json({
      token: 'new_mock_jwt_token_12345',
      refreshToken: 'new_mock_refresh_token_12345',
    });
  }),

  // User profile
  http.get('/api/me', () => {
    return HttpResponse.json({
      user: mockUsers[0],
      organization: {
        name: 'CyberScope Team',
        slug: 'cyberscope-team',
      },
      preferences: {
        theme: 'dark',
        language: 'tr',
        notifications: true,
      },
    });
  }),

  // Scans
  http.get('/api/scans', () => {
    return HttpResponse.json({
      items: mockScans,
      total: mockScans.length,
      page: 1,
      limit: 10,
    });
  }),

  http.post('/api/scans', () => {
    const newScan = {
      id: Date.now(),
      name: 'New Scan',
      type: 'domain',
      targets: ['example.com'],
      status: 'queued',
      risk: 0,
      startedAt: new Date().toISOString(),
      finishedAt: null,
      findings: 0,
      providers: ['VirusTotal'],
    };
    
    mockScans.push(newScan);
    
    return HttpResponse.json(newScan);
  }),

  http.get('/api/scans/:id', ({ params }) => {
    const scan = mockScans.find(s => s.id === parseInt(params.id));
    if (!scan) {
      return new HttpResponse(null, { status: 404 });
    }

    const detailedScan = {
      ...scan,
      findings: [
        {
          id: 1,
          title: 'Open Port Detected',
          description: 'Port 22 (SSH) is open and accessible',
          severity: 'medium',
          category: 'network',
          source: 'Shodan',
          createdAt: scan.startedAt,
        },
        {
          id: 2,
          title: 'SSL Certificate Expired',
          description: 'SSL certificate will expire in 30 days',
          severity: 'low',
          category: 'ssl',
          source: 'VirusTotal',
          createdAt: scan.startedAt,
        },
      ],
      timeline: [
        {
          id: 1,
          event: 'Scan started',
          timestamp: scan.startedAt,
          details: 'Initializing scan for target',
        },
        {
          id: 2,
          event: 'Data collection',
          timestamp: new Date(Date.parse(scan.startedAt) + 60000).toISOString(),
          details: 'Collecting data from external sources',
        },
        {
          id: 3,
          event: 'Analysis complete',
          timestamp: scan.finishedAt,
          details: 'Scan analysis completed successfully',
        },
      ],
    };

    return HttpResponse.json(detailedScan);
  }),

  // Entities
  http.post('/api/entities/email/breach', () => {
    return HttpResponse.json({
      email: 'test@example.com',
      breaches: [
        {
          name: 'Adobe Breach 2013',
          date: '2013-10-04',
          description: 'Adobe systems compromised',
          dataClasses: ['email addresses', 'passwords', 'usernames'],
          pwnCount: 153445165,
        },
        {
          name: 'LinkedIn Breach 2012',
          date: '2012-05-05',
          description: 'LinkedIn database compromised',
          dataClasses: ['email addresses', 'passwords'],
          pwnCount: 165000000,
        },
      ],
      recommendations: [
        'Change password immediately',
        'Enable two-factor authentication',
        'Use unique passwords for each service',
        'Monitor accounts for suspicious activity',
      ],
    });
  }),

  http.get('/api/entities/domain/:domain', ({ params }) => {
    return HttpResponse.json({
      domain: params.domain,
      whois: {
        registrar: 'Google LLC',
        created: '1997-09-15',
        updated: '2023-09-13',
        expires: '2028-09-14',
        status: 'active',
      },
      vt: {
        reputation: 0,
        categories: ['search engines'],
        lastAnalysisStats: {
          harmless: 85,
          malicious: 0,
          suspicious: 0,
          undetected: 15,
        },
      },
      geo: {
        country: 'US',
        city: 'Mountain View',
        coordinates: [37.4056, -122.0775],
      },
      blacklist: [],
      risk: 2,
    });
  }),

  http.get('/api/entities/ip/:ip', ({ params }) => {
    return HttpResponse.json({
      ip: params.ip,
      shodan: {
        country: 'US',
        city: 'Mountain View',
        org: 'Google LLC',
        ports: [80, 443, 8080],
        hostnames: ['google.com'],
      },
      ports: [
        {
          port: 80,
          service: 'http',
          product: 'nginx',
          version: '1.18.0',
        },
        {
          port: 443,
          service: 'https',
          product: 'nginx',
          version: '1.18.0',
        },
      ],
      cves: [
        {
          id: 'CVE-2021-23017',
          severity: 'medium',
          description: 'nginx vulnerability in resolver',
          cvss: 5.3,
        },
      ],
      risk: 4,
    });
  }),

  // Reports
  http.get('/api/reports', () => {
    return HttpResponse.json({
      items: mockReports,
      total: mockReports.length,
    });
  }),

  http.get('/api/reports/:id', ({ params }) => {
    const report = mockReports.find(r => r.id === parseInt(params.id));
    if (!report) {
      return new HttpResponse(null, { status: 404 });
    }

    return HttpResponse.json(report);
  }),

  // Notifications
  http.get('/api/notifications', () => {
    return HttpResponse.json({
      items: mockNotifications,
      total: mockNotifications.length,
    });
  }),

  http.patch('/api/notifications/:id', ({ params }) => {
    const notification = mockNotifications.find(n => n.id === parseInt(params.id));
    if (notification) {
      notification.read = true;
    }
    return HttpResponse.json(notification);
  }),

  // API Keys
  http.get('/api/apikeys', () => {
    return HttpResponse.json({
      items: mockApiKeys,
      total: mockApiKeys.length,
    });
  }),

  http.post('/api/apikeys', () => {
    const newKey = {
      id: Date.now(),
      name: 'New API Key',
      key: `csk_${Math.random().toString(36).substring(2, 15)}`,
      scopes: ['read'],
      lastUsed: null,
      createdAt: new Date().toISOString(),
      status: 'active',
    };
    
    mockApiKeys.push(newKey);
    
    return HttpResponse.json(newKey);
  }),

  http.delete('/api/apikeys/:id', ({ params }) => {
    const index = mockApiKeys.findIndex(k => k.id === parseInt(params.id));
    if (index !== -1) {
      mockApiKeys.splice(index, 1);
    }
    return HttpResponse.json({ message: 'API key deleted' });
  }),

  // Users (admin only)
  http.get('/api/users', () => {
    return HttpResponse.json({
      items: mockUsers,
      total: mockUsers.length,
    });
  }),

  http.get('/api/audit', () => {
    return HttpResponse.json({
      items: [
        {
          id: 1,
          user: 'Melisa Bayramoğlu',
          action: 'login',
          timestamp: '2024-01-15T10:30:00Z',
          ip: '192.168.1.100',
          details: 'Successful login',
        },
        {
          id: 2,
          user: 'Gül Yasemin',
          action: 'scan_created',
          timestamp: '2024-01-15T09:15:00Z',
          ip: '192.168.1.101',
          details: 'Created new domain scan',
        },
      ],
      total: 2,
    });
  }),

  // Settings
  http.get('/api/settings/organization', () => {
    return HttpResponse.json({
      name: 'CyberScope Team',
      slug: 'cyberscope-team',
      description: 'Advanced cybersecurity research team',
      website: 'https://cyberscope.com',
      industry: 'Technology',
      size: '10-50',
    });
  }),

  http.get('/api/settings/integrations', () => {
    return HttpResponse.json({
      providers: [
        {
          name: 'VirusTotal',
          enabled: true,
          apiKey: 'vt_1234567890abcdef',
          lastSync: '2024-01-15T08:00:00Z',
        },
        {
          name: 'Shodan',
          enabled: true,
          apiKey: 'shodan_1234567890abcdef',
          lastSync: '2024-01-15T08:00:00Z',
        },
        {
          name: 'HaveIBeenPwned',
          enabled: true,
          apiKey: 'hibp_1234567890abcdef',
          lastSync: '2024-01-15T08:00:00Z',
        },
      ],
    });
  }),
];
