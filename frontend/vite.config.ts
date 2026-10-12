import { defineConfig, Plugin } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import fs from "fs";

// Render Static Sites serve /404.html for unknown paths. Copying index.html to
// 404.html makes a reload on e.g. /employees load the SPA instead of "Not Found",
// even if the dashboard rewrite rule (/* -> /index.html) is missing.
const spaFallback = (): Plugin => ({
  name: "spa-404-fallback",
  apply: "build",
  closeBundle() {
    const dist = path.resolve(__dirname, "dist");
    const index = path.join(dist, "index.html");
    if (fs.existsSync(index)) fs.copyFileSync(index, path.join(dist, "404.html"));
  },
});

// https://vitejs.dev/config/
export default defineConfig(() => ({
  server: {
    host: "::",
    port: 8080,
  },
  plugins: [react(), spaFallback()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
