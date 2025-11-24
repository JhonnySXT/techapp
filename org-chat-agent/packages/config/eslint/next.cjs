const base = require("./base.cjs");

module.exports = {
  ...base,
  extends: [
    "./base.cjs",
    "next/core-web-vitals",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended",
  ],
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint", "react-refresh"],
  rules: {
    ...base.rules,
    "@typescript-eslint/explicit-function-return-type": "off",
    "react-refresh/only-export-components":
      process.env.NODE_ENV === "production" ? "error" : "warn",
  },
};






