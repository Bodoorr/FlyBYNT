import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api/<service>/... is forwarded to that service on localhost (nginx does the same in Docker),
// so the browser only ever talks to one origin and the services need no CORS setup.
const service = (port) => ({
  target: `http://localhost:${port}`,
  changeOrigin: true,
  rewrite: (path) => path.replace(/^\/api\/[a-z]+/, ''),
});

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/flight': service(8081),
      '/api/seat': service(8082),
      '/api/booking': service(8083),
      '/api/passenger': service(8084),
      '/api/auth': service(8085),
    },
  },
});
