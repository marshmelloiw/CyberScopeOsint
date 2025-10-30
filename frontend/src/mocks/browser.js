import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

// This configures a Service Worker with the given request handlers.
export const worker = setupWorker(...handlers);

// Start the worker when the app is in development mode
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCKS !== 'false') {
  worker.start({
    onUnhandledRequest: 'bypass', // Don't warn about unhandled requests
  });
  
  console.log('🔧 MSW Mock Server started in development mode');
}
