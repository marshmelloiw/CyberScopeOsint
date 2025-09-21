import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { ArrowLeft, ArrowRight, Check, Globe, Mail, MapPin, Users, FileText } from 'lucide-react';
import { cn } from '../../lib/utils';

const NewScan = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: '',
    targets: [],
    providers: [],
    schedule: 'immediate',
    notifications: true,
    tags: [],
  });

  const scanTypes = [
    { id: 'domain', name: 'Domain Analysis', icon: Globe, description: 'Analyze domain security, DNS, and reputation' },
    { id: 'email', name: 'Email Breach Check', icon: Mail, description: 'Check email addresses against data breaches' },
    { id: 'ip', name: 'IP Address Analysis', icon: MapPin, description: 'Analyze IP addresses for threats and vulnerabilities' },
    { id: 'social', name: 'Social Media Monitor', icon: Users, description: 'Monitor social media accounts for threats' },
  ];

  const providers = {
    domain: ['VirusTotal', 'Shodan', 'Whois', 'AbuseIPDB', 'URLVoid'],
    email: ['HaveIBeenPwned', 'DeHashed', 'Intelligence X', 'LeakCheck'],
    ip: ['Shodan', 'VirusTotal', 'AbuseIPDB', 'IPQualityScore', 'IP2Location'],
    social: ['Twitter', 'LinkedIn', 'GitHub', 'Reddit', 'Telegram'],
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleTargetsChange = (value) => {
    const targets = value.split('\n').filter(target => target.trim());
    setFormData(prev => ({ ...prev, targets }));
  };

  const handleProviderToggle = (provider) => {
    setFormData(prev => ({
      ...prev,
      providers: prev.providers.includes(provider)
        ? prev.providers.filter(p => p !== provider)
        : [...prev.providers, provider]
    }));
  };

  const handleTagsChange = (value) => {
    const tags = value.split(',').map(tag => tag.trim()).filter(tag => tag);
    setFormData(prev => ({ ...prev, tags }));
  };

  const nextStep = () => {
    if (currentStep < 3) setCurrentStep(currentStep + 1);
  };

  const prevStep = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
  };

  const handleSubmit = async () => {
    // Mock API call
    console.log('Creating scan:', formData);
    
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Redirect to scans list
    navigate('/scans');
  };

  const isStepValid = () => {
    switch (currentStep) {
      case 1:
        return formData.name && formData.type && formData.targets.length > 0;
      case 2:
        return formData.providers.length > 0;
      case 3:
        return true;
      default:
        return false;
    }
  };

  const renderStep1 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Basic Information</h3>
        <p className="text-surface-muted">Provide basic details about your scan</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Scan Name</label>
          <Input
            placeholder="e.g., Domain Security Assessment - google.com"
            value={formData.name}
            onChange={(e) => handleInputChange('name', e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Description</label>
          <textarea
            placeholder="Optional description of the scan purpose..."
            value={formData.description}
            onChange={(e) => handleInputChange('description', e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            rows={3}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Scan Type</label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {scanTypes.map((type) => (
              <div
                key={type.id}
                className={cn(
                  'p-4 border rounded-lg cursor-pointer transition-all',
                  formData.type === type.id
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-border hover:border-primary-400'
                )}
                onClick={() => handleInputChange('type', type.id)}
              >
                <div className="flex items-center space-x-3">
                  <type.icon className="h-5 w-5 text-primary-400" />
                  <div>
                    <p className="font-medium text-white">{type.name}</p>
                    <p className="text-sm text-surface-muted">{type.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">
            Targets (one per line)
          </label>
          <textarea
            placeholder={formData.type === 'domain' ? 'google.com\nfacebook.com' : 
                       formData.type === 'email' ? 'test@example.com\nadmin@company.com' :
                       formData.type === 'ip' ? '8.8.8.8\n1.1.1.1' : '@username1\n@username2'}
            onChange={(e) => handleTargetsChange(e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white placeholder:text-surface-muted focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            rows={4}
          />
          <p className="text-sm text-surface-muted mt-1">
            {formData.targets.length} target(s) specified
          </p>
        </div>
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Data Sources</h3>
        <p className="text-surface-muted">Select which intelligence providers to use</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Available Providers</label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {providers[formData.type]?.map((provider) => (
              <div
                key={provider}
                className={cn(
                  'p-3 border rounded-lg cursor-pointer transition-all',
                  formData.providers.includes(provider)
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-border hover:border-primary-400'
                )}
                onClick={() => handleProviderToggle(provider)}
              >
                <div className="flex items-center justify-between">
                  <span className="text-white">{provider}</span>
                  {formData.providers.includes(provider) && (
                    <Check className="h-5 w-5 text-primary-500" />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="p-4 bg-surface-panel/50 rounded-lg">
          <p className="text-sm text-surface-muted">
            <strong>Selected:</strong> {formData.providers.length} provider(s)
          </p>
          <p className="text-sm text-surface-muted mt-1">
            More providers = more comprehensive results, but longer scan time
          </p>
        </div>
      </div>
    </div>
  );

  const renderStep3 = () => (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold text-white mb-2">Schedule & Settings</h3>
        <p className="text-surface-muted">Configure when and how to run the scan</p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-white mb-2">Schedule</label>
          <select
            value={formData.schedule}
            onChange={(e) => handleInputChange('schedule', e.target.value)}
            className="w-full px-3 py-2 bg-surface-panel border border-surface-border rounded-lg text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          >
            <option value="immediate">Run immediately</option>
            <option value="scheduled">Schedule for later</option>
            <option value="recurring">Set up recurring scan</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Notifications</label>
          <div className="flex items-center space-x-3">
            <input
              type="checkbox"
              id="notifications"
              checked={formData.notifications}
              onChange={(e) => handleInputChange('notifications', e.target.checked)}
              className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="notifications" className="text-white">
              Send notifications when scan completes
            </label>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">Tags (optional)</label>
          <Input
            placeholder="security, domain, assessment (comma separated)"
            value={formData.tags.join(', ')}
            onChange={(e) => handleTagsChange(e.target.value)}
          />
        </div>

        <div className="p-4 bg-surface-panel/50 rounded-lg">
          <h4 className="font-medium text-white mb-2">Scan Summary</h4>
          <div className="space-y-2 text-sm text-surface-muted">
            <p><strong>Name:</strong> {formData.name}</p>
            <p><strong>Type:</strong> {formData.type}</p>
            <p><strong>Targets:</strong> {formData.targets.length}</p>
            <p><strong>Providers:</strong> {formData.providers.length}</p>
            <p><strong>Schedule:</strong> {formData.schedule}</p>
          </div>
        </div>
      </div>
    </div>
  );

  const steps = [
    { number: 1, title: 'Basic Info', icon: FileText },
    { number: 2, title: 'Data Sources', icon: Globe },
    { number: 3, title: 'Settings', icon: Check },
  ];

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center space-x-4">
        <Button variant="ghost" onClick={() => navigate('/scans')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold text-white">New Scan</h1>
          <p className="text-surface-muted">Create a new security scan</p>
        </div>
      </div>

      {/* Progress steps */}
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            {steps.map((step, index) => (
              <div key={step.number} className="flex items-center">
                <div className={cn(
                  'flex items-center justify-center w-10 h-10 rounded-full border-2',
                  currentStep >= step.number
                    ? 'border-primary-500 bg-primary-500 text-white'
                    : 'border-surface-border text-surface-muted'
                )}>
                  {currentStep > step.number ? (
                    <Check className="h-5 w-5" />
                  ) : (
                    <step.icon className="h-5 w-5" />
                  )}
                </div>
                {index < steps.length - 1 && (
                  <div className={cn(
                    'w-16 h-0.5 mx-4',
                    currentStep > step.number ? 'bg-primary-500' : 'bg-surface-border'
                  )} />
                )}
              </div>
            ))}
          </div>
          <div className="flex justify-between mt-4">
            {steps.map((step) => (
              <span
                key={step.number}
                className={cn(
                  'text-sm',
                  currentStep >= step.number ? 'text-primary-400' : 'text-surface-muted'
                )}
              >
                {step.title}
              </span>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Step content */}
      <Card>
        <CardContent className="p-6">
          {currentStep === 1 && renderStep1()}
          {currentStep === 2 && renderStep2()}
          {currentStep === 3 && renderStep3()}
        </CardContent>
      </Card>

      {/* Navigation */}
      <div className="flex justify-between">
        <Button
          variant="outline"
          onClick={prevStep}
          disabled={currentStep === 1}
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Previous
        </Button>

        <div className="flex space-x-3">
          {currentStep < 3 ? (
            <Button
              onClick={nextStep}
              disabled={!isStepValid()}
            >
              Next
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          ) : (
            <Button
              onClick={handleSubmit}
              disabled={!isStepValid()}
              className="bg-success hover:bg-success/80"
            >
              Create Scan
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default NewScan;
