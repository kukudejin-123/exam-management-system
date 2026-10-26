import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 后端跑在 8080，开发时代理过去，前端无感
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/actuator': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true
  }
})
