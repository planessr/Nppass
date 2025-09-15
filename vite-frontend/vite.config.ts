 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
 import legacy from '@vitejs/plugin-legacy'
  
 export default defineConfig({
   plugins: [
     react(),
     legacy({
       // 明确指定支持 BigInt 的浏览器版本 
       targets: [
         'chrome >= 80',
         'firefox >= 70',
         'safari >= 14',
         'edge >= 80'
       ],
       // 添加现代浏览器支持 
       modernPolyfills: true,
       // 添加必要的 polyfills 
       additionalLegacyPolyfills: ['big-integer']
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
     // 仅限本地访问或指定 IP 
     host: 'localhost',
     // 或指定 IP: host: '192.168.x.x'
   },
   build: {
     outDir: 'dist',
     sourcemap: false,
     // 启用压缩优化 
     minify: 'terser',
     terserOptions: {
       compress: {
         drop_console: true, // 生产环境移除 console 
       }
     },
     rollupOptions: {
       // 启用摇树优化 
       treeshake: true,
       output: {
         // 代码分割优化 
         manualChunks(id) {
           if (id.includes('node_modules')) {
             return 'vendor';
           }
         }
       }
     },
     // 明确构建目标 
     target: ['es2020', 'chrome80', 'edge80', 'firefox70', 'safari14']
   },
   // 添加 ESBuild 配置 
   esbuild: {
     target: 'es2020'
   }
 });