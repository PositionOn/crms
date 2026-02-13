import { defineConfig } from 'vite'
// @ts-ignore
import vue from '@vitejs/plugin-vue'
import electron from 'vite-plugin-electron'
import renderer from 'vite-plugin-electron-renderer'

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    electron({
      entry: 'electron/main.ts'
    }),
    renderer()
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8089',
        changeOrigin: true
      }
    }
  }
})

