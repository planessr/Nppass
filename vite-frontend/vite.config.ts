 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
  
 export default defineConfig({
   plugins: [
     react(), // 移除legacy插件 
   ],
   base: './',    
   resolve: {
     alias: {
       "@": path.resolve(__dirname, "./src"),
     },
   },
   server: {
     port: 3000,
     host: 'localhost' // 安全优化 
   },
   build: {
     outDir: 'dist',
     sourcemap: false,
     minify: 'terser', // 启用压缩 
     terserOptions: {
       compress: {
         drop_console: true, // 生产环境移除console 
       }
     },
     rollupOptions: {
       treeshake: true, // 启用摇树优化 
       output: {
         manualChunks(id) {
           if (id.includes('node_modules')) {
             return 'vendor'; // 代码分割优化 
           }
         }
       }
     },
     // 设置现代浏览器目标 
     target: ['es2020', 'chrome80', 'edge80', 'firefox70', 'safari14']
   },
   esbuild: {
     target: 'es2020' // 统一转译目标 
   }
 });