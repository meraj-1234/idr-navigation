/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        idr: {
          bg: '#0a0d14',
          panel: '#111622',
          border: '#1e2638',
          accent: '#3b82f6',
          cyan: '#06b6d4',
          amber: '#f59e0b',
          rose: '#f43f5e',
          emerald: '#10b981',
          textMuted: '#94a3b8',
        }
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Menlo', 'Courier New', 'monospace'],
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
