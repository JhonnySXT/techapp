const base = require("./base.cjs");

module.exports = {
  ...base,
  extends: ["./base.cjs", "plugin:@typescript-eslint/recommended"],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    sourceType: "module",
  },
  plugins: ["@typescript-eslint"],
  rules: {
    ...base.rules,
    "@typescript-eslint/no-explicit-any": "off",
  },
};






