 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
 import legacy from '@vitejs/plugin-legacy'
  
 export default defineConfig({
   plugins: [
     react(),
     legacy({
       targets: ['chrome>=87', 'firefox>=78', 'safari>=15', 'edge>=89'],
       modernPolyfills: ['es.bigint', 'es.object.entries'],
       polyfills: ['es.bigint.constructor']
     })
   ],
   base: './',
   resolve: { alias: { "@": path.resolve(__dirname, "./src") } },
   server: { port: 3000, host: '0.0.0.0' },
   build: {
     outDir: 'dist',
     sourcemap: false,
     minify: false,
     target: 'es2020',
     rollupOptions: {
       treeshake: false,
       output: {
         generatedCode: 'es2020',
         preserveModules: false, // 必须关闭 
         preserveEntrySignatures: 'strict' // 严格模式
       }
     }
   },
   esbuild: {
     target: 'es2020',
     supported: { 'bigint': true }
   }
 });