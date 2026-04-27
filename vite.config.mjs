import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "webapp/assets",
    emptyOutDir: true,
    cssCodeSplit: false,
    rollupOptions: {
      input: "frontend/src/main.jsx",
      output: {
        entryFileNames: "bank-web.js",
        chunkFileNames: "bank-web-[name].js",
        assetFileNames: (assetInfo) => {
          if (assetInfo.name && assetInfo.name.endsWith(".css")) {
            return "bank-web.css";
          }
          return "bank-web-[name][extname]";
        }
      }
    }
  }
});
