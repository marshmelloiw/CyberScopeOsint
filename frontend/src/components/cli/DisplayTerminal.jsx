import React, { useEffect, useRef, useState } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

const DisplayTerminal = ({ logs, scanId, status }) => {
  const terminalRef = useRef(null);
  const xtermRef = useRef(null);
  const fitAddonRef = useRef(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const resizeTimerRef = useRef(null);

  const safeFit = () => {
    try {
      const container = terminalRef.current;
      const addon = fitAddonRef.current;
      if (!container || !addon) return;

      // Only fit when container is laid out and visible
      const width = container.clientWidth;
      const height = container.clientHeight;
      if (width > 0 && height > 0) {
        addon.fit();
      } else {
        // Retry next frame until it has size (e.g., after animations/tabs)
        requestAnimationFrame(safeFit);
      }
    } catch (_) {
      // Renderer may not be ready yet; retry on next frame
      requestAnimationFrame(safeFit);
    }
  };

  useEffect(() => {
    if (!terminalRef.current || isInitialized) return;

    // Initialize terminal
    const terminal = new Terminal({
      cursorBlink: false,
      cursorStyle: 'block',
      fontSize: 13,
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
      disableStdin: true, // Make it read-only
      rows: 20,
    });

    const fitAddon = new FitAddon();
    terminal.loadAddon(fitAddon);

    terminal.open(terminalRef.current);
    safeFit();
    
    xtermRef.current = terminal;
    fitAddonRef.current = fitAddon;
    setIsInitialized(true);

    // Write initial content
    terminal.writeln('\x1b[1;32m╔═══════════════════════════════════════════════════════╗\x1b[0m');
    terminal.writeln('\x1b[1;32m║\x1b[0m  \x1b[1;36mCyberScope - Security Scan Execution\x1b[0m              \x1b[1;32m║\x1b[0m');
    terminal.writeln('\x1b[1;32m╚═══════════════════════════════════════════════════════╝\x1b[0m');
    terminal.writeln('');

    // Disable any input
    terminal.onData(() => {
      // Do nothing - read-only terminal
    });

    // Window resize
    const handleResize = () => {
      if (resizeTimerRef.current) {
        clearTimeout(resizeTimerRef.current);
      }
      resizeTimerRef.current = setTimeout(() => {
        safeFit();
      }, 100);
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (resizeTimerRef.current) {
        clearTimeout(resizeTimerRef.current);
      }
      terminal.dispose();
    };
  }, [isInitialized]);

  // Update terminal with logs
  useEffect(() => {
    if (!xtermRef.current || !logs || logs.length === 0) return;

    const terminal = xtermRef.current;
    
    // Clear and rewrite all logs
    terminal.clear();
    terminal.writeln('\x1b[1;32m╔═══════════════════════════════════════════════════════╗\x1b[0m');
    terminal.writeln('\x1b[1;32m║\x1b[0m  \x1b[1;36mCyberScope - Security Scan Execution\x1b[0m              \x1b[1;32m║\x1b[0m');
    terminal.writeln('\x1b[1;32m╚═══════════════════════════════════════════════════════╝\x1b[0m');
    terminal.writeln('');
    terminal.writeln(`\x1b[33mScan ID:\x1b[0m ${scanId || 'N/A'}`);
    terminal.writeln(`\x1b[33mStatus:\x1b[0m ${status || 'RUNNING'}`);
    terminal.writeln('');

    // Write logs
    logs.forEach((log) => {
      const time = new Date(log.timestamp).toLocaleTimeString();
      let colorCode = '\x1b[0m'; // Default
      
      if (log.message.includes('✓') || log.message.includes('completed')) {
        colorCode = '\x1b[32m'; // Green
      } else if (log.message.includes('✗') || log.message.includes('Error') || log.message.includes('error')) {
        colorCode = '\x1b[31m'; // Red
      } else if (log.message.includes('Querying') || log.message.includes('Processing')) {
        colorCode = '\x1b[33m'; // Yellow
      }
      
      terminal.writeln(`${colorCode}[${time}] ${log.message}\x1b[0m`);
    });

    // Scroll to bottom
    terminal.scrollToBottom();
  }, [logs, scanId, status]);

  // Fit on mount
  useEffect(() => {
    if (fitAddonRef.current && terminalRef.current) {
      setTimeout(() => {
        safeFit();
      }, 100);
    }
  }, [isInitialized]);

  return (
    <div className="w-full h-full flex flex-col">
      <div className="mb-2 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <div className="w-3 h-3 rounded-full bg-red-500"></div>
          <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
          <div className="w-3 h-3 rounded-full bg-green-500"></div>
          <span className="ml-3 text-sm text-surface-muted font-mono">CyberScope Scanner</span>
        </div>
        {status && (
          <span className={`text-xs px-2 py-1 rounded ${
            status === 'COMPLETED' ? 'bg-success/20 text-success' :
            status === 'FAILED' ? 'bg-danger/20 text-danger' :
            'bg-warning/20 text-warning'
          }`}>
            {status}
          </span>
        )}
      </div>
      <div 
        ref={terminalRef} 
        className="flex-1 border border-gray-700 rounded-lg overflow-hidden bg-[#1e1e1e]"
        style={{ minHeight: '400px' }}
      />
    </div>
  );
};

export default DisplayTerminal;

