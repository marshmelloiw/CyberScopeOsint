# CyberScope CLI - Frontend UX/UI Implementation Plan

## 🎯 Genel Yaklaşım

CyberScope'a CLI özelliği eklerken **iki farklı kullanım senaryosu** olacak:

1. **Web Terminal UI** - Frontend'de terminal benzeri arayüz
2. **Backend CLI** - Standalone terminal komutları

---

## 📐 Mimari Tasarım

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   Terminal Component (xterm.js)                      │   │
│  │   - Komut girişi                                      │   │
│  │   - Sonuç görüntüleme                                │   │
│  │   - History ve auto-completion                       │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │ WebSocket / HTTP                       │
└────────────────────┼─────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot)                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   CLI Command Controller                              │   │
│  │   - HTTP Endpoints                                    │   │
│  │   - WebSocket Handler (opsiyonel)                    │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
│  ┌─────────────────▼────────────────────────────────────┐   │
│  │   Spring Shell Commands                               │   │
│  │   - OSINT Commands                                    │   │
│  │   - User Management                                   │   │
│  │   - API Key Management                                │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎨 Frontend UX/UI Tasarımı

### 1. Terminal Component Ekranı

```
┌─────────────────────────────────────────────────────────┐
│  CyberScope                                  [×] [─] [□] │
├─────────────────────────────────────────────────────────┤
│  Dashboard | Scans | Entities | Reports | ... | CLI 📟  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ╔══════════════════════════════════════════════════╗   │
│  ║  CyberScope CLI v1.0.0                           ║   │
│  ║  Type 'help' for available commands              ║   │
│  ║                                                   ║   │
│  ║  shell:> █                                        ║   │
│  ╚══════════════════════════════════════════════════╝   │
│                                                          │
│  [Quick Actions]                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                │
│  │ Scan IP  │ │ Domain   │ │ Email    │                │
│  └──────────┘ └──────────┘ └──────────┘                │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 2. Sidebar'a CLI Ekleme

```javascript
// Sidebar navigation'a ekleme
const navigation = [
  // ... mevcut items
  { name: 'CLI', href: '/cli', icon: Terminal },  // Yeni item
];
```

---

## 🔧 Implementation Detayları

### Backend: CLI Command Controller

#### 1. HTTP Endpoint Yaklaşımı

```java
package osint.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import osint.shell.command.OsintScanCommands;
import java.util.Map;

@RestController
@RequestMapping("/api/cli")
public class CliController {
    
    @Autowired
    private OsintScanCommands osintCommands;
    
    @PostMapping("/execute")
    public ResponseEntity<CliResponse> executeCommand(
        @RequestBody CliRequest request
    ) {
        String command = request.getCommand();
        String[] args = request.getArgs();
        
        // Komutu parse et ve ilgili command method'unu çağır
        String result = executeCliCommand(command, args);
        
        return ResponseEntity.ok(new CliResponse(result, true));
    }
    
    private String executeCliCommand(String command, String[] args) {
        // Komut routing
        switch (command) {
            case "shodan":
                return osintCommands.shodanQuery(args[0], args[1]);
            case "virustotal":
                return osintCommands.virusTotalQuery(args[0], args[1]);
            // ... diğer komutlar
            default:
                return "Command not found. Type 'help' for available commands.";
        }
    }
}

class CliRequest {
    private String command;
    private String[] args;
    // getters/setters
}

class CliResponse {
    private String output;
    private boolean success;
    // getters/setters
}
```

#### 2. WebSocket Yaklaşımı (İnteraktif Terminal için)

```java
package osint.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CliWebSocketController {
    
    @MessageMapping("/cli/command")
    @SendTo("/topic/cli/output")
    public CliMessage handleCommand(CliMessage message) {
        String command = message.getCommand();
        String result = executeCommand(command);
        return new CliMessage(result, "output");
    }
}
```

---

### Frontend: Terminal Component

#### 1. Package Installation

```bash
cd frontend
npm install xterm xterm-addon-fit xterm-addon-web-links
```

#### 2. Terminal Component Oluşturma

```jsx
// frontend/src/features/cli/Terminal.jsx
import React, { useEffect, useRef, useState } from 'react';
import { Terminal as XTerm } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { WebLinksAddon } from 'xterm-addon-web-links';
import 'xterm/css/xterm.css';
import axios from '../../lib/axios';

const Terminal = () => {
  const terminalRef = useRef(null);
  const xtermRef = useRef(null);
  const fitAddonRef = useRef(null);
  const [commandHistory, setCommandHistory] = useState([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const currentCommandRef = useRef('');

  useEffect(() => {
    // Terminal'i initialize et
    const terminal = new XTerm({
      cursorBlink: true,
      fontSize: 14,
      fontFamily: 'Consolas, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
        cursor: '#aeafad',
        black: '#000000',
        red: '#cd3131',
        green: '#0dbc79',
        yellow: '#e5e510',
        blue: '#2472c8',
        magenta: '#bc3fbc',
        cyan: '#11a8cd',
        white: '#e5e5e5',
      },
    });

    const fitAddon = new FitAddon();
    terminal.loadAddon(fitAddon);
    terminal.loadAddon(new WebLinksAddon());

    terminal.open(terminalRef.current);
    fitAddon.fit();
    
    xtermRef.current = terminal;
    fitAddonRef.current = fitAddon;

    // Welcome message
    terminal.writeln('\x1b[1;32mCyberScope CLI v1.0.0\x1b[0m');
    terminal.writeln('Type \x1b[1;33mhelp\x1b[0m for available commands');
    terminal.writeln('');
    showPrompt(terminal);

    // Input handling
    let currentLine = '';
    terminal.onData((data) => {
      if (data === '\r') { // Enter
        terminal.writeln('');
        handleCommand(currentLine.trim(), terminal);
        setCommandHistory(prev => [...prev, currentLine.trim()]);
        currentCommandRef.current = '';
        currentLine = '';
        showPrompt(terminal);
      } else if (data === '\x7f') { // Backspace
        if (currentLine.length > 0) {
          currentLine = currentLine.slice(0, -1);
          terminal.write('\b \b');
        }
      } else if (data === '\x1b[A') { // Up arrow
        // History navigation
        if (historyIndex > 0) {
          const newIndex = historyIndex - 1;
          setHistoryIndex(newIndex);
          currentLine = commandHistory[commandHistory.length - 1 - newIndex];
          terminal.write('\r\x1b[K'); // Clear line
          showPrompt(terminal);
          terminal.write(currentLine);
        }
      } else {
        currentLine += data;
        terminal.write(data);
      }
    });

    // Window resize
    window.addEventListener('resize', () => fitAddon.fit());

    return () => {
      terminal.dispose();
      window.removeEventListener('resize', () => fitAddon.fit());
    };
  }, []);

  const showPrompt = (terminal) => {
    terminal.write('\x1b[1;36mshell:>\x1b[0m ');
  };

  const handleCommand = async (command, terminal) => {
    if (!command) return;

    // Built-in commands
    if (command === 'help') {
      showHelp(terminal);
      return;
    }

    if (command === 'clear') {
      terminal.clear();
      showPrompt(terminal);
      return;
    }

    // Parse command
    const parts = command.split(' ');
    const cmd = parts[0];
    const args = parts.slice(1);

    // Send to backend
    try {
      terminal.write('\x1b[33mExecuting...\x1b[0m\r\n');
      
      const response = await axios.post('/api/cli/execute', {
        command: cmd,
        args: args,
      });

      if (response.data.success) {
        terminal.writeln(response.data.output);
      } else {
        terminal.writeln(`\x1b[31mError: ${response.data.output}\x1b[0m`);
      }
    } catch (error) {
      terminal.writeln(`\x1b[31mError: ${error.response?.data?.message || error.message}\x1b[0m`);
    }
  };

  const showHelp = (terminal) => {
    const helpText = `
\x1b[1;32mAvailable Commands:\x1b[0m

\x1b[1;36mOSINT Operations:\x1b[0m
  shodan --target <ip|domain> [--type ip|domain|search]
  virustotal --target <ip|domain> [--type ip|domain]
  breach-check --email <email>

\x1b[1;36mUser Management:\x1b[0m
  user create --email <email> --password <password>
  user list
  user delete --email <email>

\x1b[1;36mAPI Key Management:\x1b[0m
  apikey set --service <shodan|virustotal|hibp> --key <key>
  apikey status

\x1b[1;36mSystem:\x1b[0m
  info          - Show system information
  health        - Check system health
  clear         - Clear terminal
  help          - Show this help message

\x1b[1;33mTip:\x1b[0m Use Tab for auto-completion and Up/Down arrows for command history.
`;
    terminal.writeln(helpText);
  };

  return (
    <div className="h-full flex flex-col bg-gray-900 p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-white">CyberScope CLI</h2>
        <div className="flex gap-2">
          <button
            onClick={() => xtermRef.current?.clear()}
            className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Clear
          </button>
        </div>
      </div>
      
      {/* Quick Actions */}
      <div className="mb-4 flex gap-2 flex-wrap">
        <button
          onClick={() => executeQuickCommand('shodan --target 8.8.8.8')}
          className="px-3 py-1 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm"
        >
          Quick: Scan IP
        </button>
        <button
          onClick={() => executeQuickCommand('apikey status')}
          className="px-3 py-1 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm"
        >
          Check API Keys
        </button>
        <button
          onClick={() => executeQuickCommand('health')}
          className="px-3 py-1 bg-gray-700 text-white rounded hover:bg-gray-600 text-sm"
        >
          System Health
        </button>
      </div>

      {/* Terminal Container */}
      <div 
        ref={terminalRef} 
        className="flex-1 border border-gray-600 rounded overflow-hidden"
        style={{ minHeight: '400px' }}
      />
    </div>
  );

  const executeQuickCommand = (command) => {
    if (xtermRef.current) {
      xtermRef.current.write(command + '\r');
    }
  };
};

export default Terminal;
```

#### 3. Route Ekleme

```javascript
// App.jsx
import Terminal from './features/cli/Terminal';

// Routes içine ekle
<Route path="cli" element={<Terminal />} />
```

---

## 🎨 UI/UX Özellikleri

### 1. Terminal Görünümü

- **Dark Theme**: CyberScope'un dark theme'i ile uyumlu
- **Syntax Highlighting**: Komut ve sonuçlar için renk kodlama
- **Responsive**: Mobil ve desktop uyumlu

### 2. Özellikler

✅ **Auto-completion**: Tab tuşu ile komut tamamlama
✅ **Command History**: Up/Down arrow ile geçmiş komutlar
✅ **Quick Actions**: Hızlı komut butonları
✅ **Help System**: `help` komutu ile yardım
✅ **Error Handling**: Hata mesajları kırmızı renkte
✅ **Loading States**: Komut çalışırken loading göstergesi

### 3. Kullanıcı Deneyimi

#### A. Komut Girişi
```
shell:> shodan --target 8.8.8.8
```

#### B. Sonuç Gösterimi
```
shell:> shodan --target 8.8.8.8
Executing...
┌─────────────────────────────────────┐
│ Shodan Scan Results                 │
├─────────────────────────────────────┤
│ IP: 8.8.8.8                         │
│ Organization: Google LLC            │
│ Country: US                         │
│ Open Ports: 80, 443                 │
└─────────────────────────────────────┘
```

#### C. Hata Durumu
```
shell:> shodan --target invalid
Error: Invalid IP address format
```

---

## 📱 Responsive Design

```jsx
// Mobil için optimize edilmiş görünüm
<div className="h-full flex flex-col">
  {/* Desktop: Full terminal */}
  <div className="hidden md:block">
    <Terminal />
  </div>
  
  {/* Mobile: Compact view */}
  <div className="md:hidden">
    <CompactTerminal />
  </div>
</div>
```

---

## 🔐 Güvenlik

### 1. Authentication
- CLI endpoint'leri protected route olmalı
- JWT token kontrolü

```java
@RestController
@RequestMapping("/api/cli")
@PreAuthorize("isAuthenticated()")
public class CliController {
    // ...
}
```

### 2. Command Validation
- Sadece izin verilen komutlar çalıştırılabilir
- SQL injection ve command injection koruması

---

## 🚀 Deployment Senaryoları

### Senaryo 1: Web + CLI Birlikte
```yaml
# application.yml
spring:
  main:
    web-application-type: servlet  # Hem web hem CLI
```

### Senaryo 2: Sadece Web Terminal
```yaml
# application.yml
spring:
  shell:
    interactive:
      enabled: false  # Backend CLI kapalı
```

Frontend terminal HTTP endpoint'leri kullanır.

---

## 📊 Örnek Kullanım Senaryoları

### Senaryo 1: Hızlı IP Taraması
1. Kullanıcı `/cli` sayfasına gider
2. Terminal açılır
3. `shodan --target 8.8.8.8` yazar
4. Sonuçlar terminal'de görüntülenir

### Senaryo 2: Batch İşlemler
1. Quick action butonları kullanılır
2. Birden fazla komut sırayla çalıştırılır
3. Sonuçlar history'de saklanır

### Senaryo 3: Sistem Yönetimi
1. Admin kullanıcısı CLI'ya girer
2. `user list` ile kullanıcıları görür
3. `apikey status` ile API key durumunu kontrol eder

---

## ✅ Özet

**Frontend'de:**
- ✅ Terminal component (xterm.js)
- ✅ Sidebar'a CLI menü item'ı
- ✅ Quick action butonları
- ✅ Command history ve auto-completion
- ✅ Dark theme ile uyumlu tasarım

**Backend'de:**
- ✅ HTTP endpoint'ler (`/api/cli/execute`)
- ✅ Spring Shell komutları
- ✅ Authentication ve authorization
- ✅ Command validation

**Kullanıcı Deneyimi:**
- ✅ Web arayüzünden terminal kullanımı
- ✅ Hızlı komut çalıştırma
- ✅ Sonuçların görselleştirilmesi
- ✅ Responsive tasarım

Bu yapı ile kullanıcılar hem web arayüzünü hem de terminal'i tek yerden kullanabilir!

