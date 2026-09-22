import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true, // Listen on all network interfaces so smartphones on local Wi-Fi can connect
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/ws-telemetry': {
        target: 'http://127.0.0.1:8080',
        ws: true,
      },
    },
  },
  define: {
    // Polyfill for sockjs-client in modern browser environments
    global: 'window',
  },
});
