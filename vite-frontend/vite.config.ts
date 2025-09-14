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
     // 新增的构建目标配置
     target: ['es2020', 'chrome89', 'edge89', 'firefox89', 'safari15'],
     // 您原有的构建配置保持不变 
     outDir: 'dist',
     sourcemap: false,
     minify: false,  
     rollupOptions: {
       treeshake: false,
     }
   }
 });