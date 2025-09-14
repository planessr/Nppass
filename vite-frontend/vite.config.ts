 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
 import legacy from '@vitejs/plugin-legacy'
  
 export default defineConfig({
   plugins: [
     react(),
     legacy({
       targets: [
         'chrome >= 87',  // 支持 BigInt 的最低版本 
         'firefox >= 78',
         'safari >= 14',
         'edge >= 88'
       ],
       modernPolyfills: ['es.bigint']  // 关键修复：为旧浏览器添加 BigInt polyfill 
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
     target: 'es2020',  // 关键修复：明确设置支持 BigInt 的构建目标 
     rollupOptions: {
       treeshake: false,
     }
   },
   esbuild: {
     target: 'es2020'  // 关键修复：确保 esbuild 也使用正确目标 
   }
 });