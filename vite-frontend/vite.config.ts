 import { defineConfig } from "vite";
 import react from "@vitejs/plugin-react";
 import path from "path";
 import legacy from '@vitejs/plugin-legacy';
  
 export default defineConfig(({ mode }) => ({
   plugins: [
     react(),
     legacy({ targets: ['defaults', 'not IE 11'] })
   ],
   base: './',
   resolve: { alias: { "@": path.resolve(__dirname, "./src") } },
   server: { port: 3000, host: '0.0.0.0' },
   build: {
     outDir: 'dist',
     sourcemap: true, // 生产环境保留sourcemap 
     minify: mode === 'production' ? 'terser' : false, // 仅生产环境压缩 
     rollupOptions: { treeshake: true }, // 启用摇树优化 
     // 移除target，由legacy插件自动处理 
   }
 }));