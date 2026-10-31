import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // Non-VITE prefix: the target is server configuration, never bundled into the client.
  const env = loadEnv(mode, '.', '');
  const target = env.API_PROXY_TARGET || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target,
          changeOrigin: true
        },
        '/actuator': {
          target,
          changeOrigin: true
        }
      }
    }
  };
});
