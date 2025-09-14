 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
 import legacy from '@vitejs/plugin-legacy'
  
 export default defineConfig({
   plugins: [
     react(),
     legacy({
       targets: [
         'chrome >= 87',
         'firefox >= 78',
         'safari >= 15',  // 关键修复：升级到 Safari 15+
         'edge >= 89'      // 关键修复：升级到 Edge 89+
       ],
       modernPolyfills: [
         'es.bigint',
         'es.object.entries'  // 增强兼容性 
       ],
       polyfills: [            // 显式添加传统浏览器 polyfill
         'es.bigint.constructor'
       ]
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
     target: 'es2020',
     rollupOptions: {
       treeshake: false,
       output: {  // 关键修复：禁用语法转换 
         generatedCode: 'es2020',
         preserveModules: true 
       }
     }
   },
   esbuild: {
     target: 'es2020',
     supported: {  // 关键修复：显式启用 BigInt
       'bigint': true 
     }
   }
 });