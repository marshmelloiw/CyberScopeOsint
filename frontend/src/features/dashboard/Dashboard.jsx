import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import StatWidget from '../../components/common/StatWidget';
import RiskBadge from '../../components/common/RiskBadge';
import useAuthStore from '../../store/auth';
import {
  AlertTriangle,
  Shield,
  Search,
  FileSearch,
} from 'lucide-react';
import {
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
} from 'recharts';
import { cn } from '../../lib/utils';

const Dashboard = () => {
  const [result, setResult] = useState(null);
  const [trend, setTrend] = useState([]);
  const [severityDist, setSeverityDist] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const { user } = useAuthStore();
  const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:8080';

  useEffect(() => {
    const fetchScore = async () => {
      try {
        const payload = {
          provider: {
            virustotal_detected: 2,
            virustotal_total: 70,
            shodan_open_ports: 3,
            shodan_high_risk_ports: 1,
            hibp_breach_count: 1
          },
          context: { asset_type: 'domain' }
        };
        const res = await fetch(`${base}/api/threat/score`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          const data = await res.json();
          setResult(data);
          const now = Date.now();
          const series = Array.from({ length: 10 }).map((_, i) => ({
            t: new Date(now - (9 - i) * 3600_000).toLocaleTimeString(),
            score: Math.max(0, Math.min(100, Math.round(data.risk_score + (Math.random() * 20 - 10))))
          }));
          setTrend(series);
          const dist = [
            { name: 'Low', value: Math.max(0, 100 - data.risk_score) },
            { name: 'Medium', value: Math.max(0, Math.min(100, data.risk_score - 30)) },
            { name: 'High', value: Math.max(0, data.risk_score > 70 ? 30 : 10) },
          ];
          setSeverityDist(dist);
        }
      } catch (e) {
        // ignore demo errors
      }
    };
    const loadIncidents = () => {
      setIncidents([
        { id: 1, time: '10:12', type: 'VT Detection', detail: 'Malware flagged in sample.zip' },
        { id: 2, time: '10:40', type: 'Shodan', detail: 'Open RDP detected on 3389' },
        { id: 3, time: '11:05', type: 'HIBP', detail: 'Email breach found for admin@example.com' },
      ]);
    };
    fetchScore();
    loadIncidents();
  }, []);

  // Mock data - in real app this would come from API
  const stats = [
    {
      title: 'Open Alerts',
      value: '12',
      change: '+3',
      changeType: 'negative',
      icon: AlertTriangle,
      trend: { direction: 'up', value: '+25%' },
    },
    {
      title: 'Avg. Risk Score',
      value: '4.2',
      change: '-0.8',
      changeType: 'positive',
      icon: Shield,
      trend: { direction: 'down', value: '-16%' },
    },
    {
      title: 'Active Scans',
      value: '5',
      change: '+2',
      changeType: 'neutral',
      icon: Search,
      trend: { direction: 'up', value: '+40%' },
    },
    {
      title: 'New Findings (24h)',
      value: '28',
      change: '+12',
      changeType: 'negative',
      icon: FileSearch,
      trend: { direction: 'up', value: '+75%' },
    },
  ];

  const recentAlerts = [
    {
      id: 1,
      severity: 'high',
      entity: 'google.com',
      source: 'VirusTotal',
      time: '2 hours ago',
      description: 'Malware detection in domain scan',
    },
    {
      id: 2,
      severity: 'medium',
      entity: '192.168.1.100',
      source: 'Shodan',
      time: '4 hours ago',
      description: 'Open port 22 detected',
    },
    {
      id: 3,
      severity: 'low',
      entity: 'test@example.com',
      source: 'HaveIBeenPwned',
      time: '6 hours ago',
      description: 'Email found in data breach',
    },
  ];

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'high':
        return 'severity-high';
      case 'medium':
        return 'severity-medium';
      case 'low':
        return 'severity-low';
      default:
        return 'severity-low';
    }
  };

  const COLORS = ['#22c55e', '#eab308', '#ef4444'];
  const isAdmin = (user?.role || '').toLowerCase() === 'admin';

  return (
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <div className="p-4 border rounded xl:col-span-1">
        <h3 className="font-semibold mb-2">AI Threat Score</h3>
        {result ? (
          <div className="flex items-center gap-3">
            <RiskBadge risk={Math.round(result.risk_score/10)} showScore={true} />
      <div>
              <div className="text-sm text-gray-600">Level: {result.risk_level}</div>
              <div className="text-sm text-gray-600">Score: {result.risk_score}</div>
            </div>
          </div>
        ) : (
          <div className="text-sm text-gray-500">Hesaplanıyor...</div>
        )}
      </div>

      <div className="p-4 border rounded xl:col-span-2">
        <h3 className="font-semibold mb-2">Risk Trend</h3>
        <div className="w-full h-56">
          <ResponsiveContainer>
            <LineChart data={trend}>
              <Line type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={2} />
              <CartesianGrid stroke="#eee" strokeDasharray="5 5" />
              <XAxis dataKey="t" hide />
              <YAxis domain={[0, 100]} />
              <Tooltip />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="p-4 border rounded xl:col-span-1">
        <h3 className="font-semibold mb-2">Severity Distribution</h3>
        <div className="w-full h-56">
          <ResponsiveContainer>
            <PieChart>
              <Pie data={severityDist} dataKey="value" nameKey="name" outerRadius={80} label>
                {severityDist.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
                </div>

      <div className="p-4 border rounded xl:col-span-2">
        <h3 className="font-semibold mb-2">Incident Logs</h3>
        <div className="space-y-2">
          {incidents.map((it) => (
            <div key={it.id} className="flex items-center justify-between border rounded px-3 py-2">
              <div className="text-sm text-gray-500 w-16">{it.time}</div>
              <div className="font-medium w-40">{it.type}</div>
              <div className="text-sm text-gray-700 flex-1">{it.detail}</div>
                </div>
          ))}
        </div>
      </div>

      {isAdmin && (
        <div className="p-4 border rounded xl:col-span-1">
          <h3 className="font-semibold mb-2">Admin Panel</h3>
          <p className="text-sm text-gray-600">Yalnızca Admin görünümü: sistem genel risk, kullanıcı yönetimi ve rapor tetikleme kısayolları burada yer alabilir.</p>
          </div>
      )}
    </div>
  );
};

export default Dashboard;
