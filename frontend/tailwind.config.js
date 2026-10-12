/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#f0f4fa',
          100: '#dce5f2',
          200: '#b9cbe6',
          300: '#8ba8d4',
          400: '#5c7fbd',
          500: '#3a61a4',
          600: '#2c4b84',
          700: '#243c6b',
          800: '#1e3157',
          900: '#1a2947',
          950: '#111c32',
        },
        charcoal: {
          800: '#1f2328',
          900: '#16191d',
          950: '#0d1117',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
}
