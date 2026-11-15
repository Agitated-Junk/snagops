/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          DEFAULT: '#0F294A',
          light: '#1B3B65'
        },
        snagNavy: '#0F294A',
        snagNavyLight: '#1B3B65',
        snagRed: '#B91C1C',
        snagGreen: '#047857',
        snagAmber: '#D97706',
        snagBg: '#F8FAFC'
      }
    },
  },
  plugins: [],
}
