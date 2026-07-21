import typography from '@tailwindcss/typography';
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        beaver: ['"Beaver Punch"', 'ui-sans-serif', 'system-ui'],
      },
    },
  },
  plugins: [typography],
};