module.exports = {
  content: [
    "./frontend/src/**/*.{js,jsx}",
    "./webapp/WEB-INF/jsp/**/*.jsp",
    "./webapp/WEB-INF/jsp/**/*.jspf"
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"]
      }
    }
  },
  safelist: [
    "bg-emerald-100",
    "text-emerald-800",
    "bg-zinc-100",
    "text-zinc-700",
    "bg-amber-100",
    "text-amber-800"
  ]
};
