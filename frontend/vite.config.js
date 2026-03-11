import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            let auth = req.headers?.authorization || req.headers?.Authorization
            if (!auth && req.url) {
              try {
                const idx = req.url.indexOf('?')
                const qs = idx >= 0 ? req.url.slice(idx + 1) : ''
                const token = new URLSearchParams(qs).get('token')
                if (token) auth = `Bearer ${token}`
              } catch (_) {}
            }
            if (auth) proxyReq.setHeader('Authorization', auth)
          })
        },
      },
    },
  },
})
