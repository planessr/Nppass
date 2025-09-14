import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import legacy from '@vitejs/plugin-legacy'

export default defineConfig({
  plugins: [
    react(),
    legacy({
      targets: ['defaults', 'not IE 11']
    })
  ],
  base: './',    
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0'
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: false,  
    rollupOptions: {
      treeshake: false,
    },
    target: 'es2021'
  }
});