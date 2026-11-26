import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // 5174 para no chocar con el portal docente (5173)
  server: { port: 5174, strictPort: true },
})