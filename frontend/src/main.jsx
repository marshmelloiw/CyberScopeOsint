import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import useUIStore from './store/ui'

// Initialize MSW in development - DISABLED FOR BACKEND TESTING
// if (import.meta.env.DEV) {
// }

// Initialize theme
useUIStore.getState().initializeTheme();

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
