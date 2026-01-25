/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-dark': '#0a0a0a',
        'surface': '#1a1a1a',
        'primary': '#a3e635',
        'primary-dark': '#84cc16',
        'txt-sub': '#888888',
      },
    },
  },
  plugins: [],
}
