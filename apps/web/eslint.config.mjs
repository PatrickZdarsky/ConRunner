import js from "@eslint/js";
import stylistic from "@stylistic/eslint-plugin";
import angular from "angular-eslint";
import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
  globalIgnores([
    "projects/**/*",
    "src/api-client/**",
    "src/auditservice-client/*",
    "src/shiftservice-client/*",
    "src/notificationservice-client/*"
  ]),
  {
    files: ["**/*.ts"],
    extends: [js.configs.recommended, tseslint.configs.recommended, angular.configs.tsRecommended],
    processor: angular.processInlineTemplates,
    plugins: { "@stylistic": stylistic },
    languageOptions: {
      parserOptions: {
        project: "./tsconfig.json",
        tsconfigRootDir: import.meta.dirname
      }
    },
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        {
          "type": "attribute",
          "prefix": "app",
          "style": "camelCase"
        }
      ],

      /* ---- ANGULAR RULES ---- */
      "@angular-eslint/component-class-suffix": "error",
      "@angular-eslint/directive-class-suffix": "error",
      "@angular-eslint/no-input-rename": "error",
      "@angular-eslint/no-inputs-metadata-property": "error",
      "@angular-eslint/no-output-on-prefix": "error",
      "@angular-eslint/no-output-rename": "error",
      "@angular-eslint/no-outputs-metadata-property": "error",
      "@angular-eslint/use-lifecycle-interface": "error",
      "@angular-eslint/use-pipe-transform-interface": "error",

      /* ---- TYPESCRIPT RULES ---- */
      "@typescript-eslint/consistent-type-definitions": "error",
      "@typescript-eslint/dot-notation": "off",
      "@typescript-eslint/explicit-member-accessibility": "off",
      "@stylistic/member-delimiter-style": [
        "error",
        {
          "multiline": {
            "delimiter": "semi",
            "requireLast": true
          },
          "singleline": {
            "delimiter": "semi",
            "requireLast": false
          }
        }
      ],
      "@typescript-eslint/member-ordering": "error",
      "@typescript-eslint/naming-convention": [
        "error",
        {
          "selector": "variable",
          "format": ["camelCase", "UPPER_CASE"],
          "leadingUnderscore": "forbid",
          "trailingUnderscore": "forbid"
        }
      ],
      "@typescript-eslint/no-empty-function": "off",
      "@typescript-eslint/no-empty-object-type": "error",
      "@typescript-eslint/no-misused-new": "error",
      "@typescript-eslint/no-non-null-assertion": "error",
      "@typescript-eslint/no-shadow": ["error", { "hoist": "all" }],
      "@typescript-eslint/no-unused-expressions": "error",
      "@typescript-eslint/prefer-function-type": "error",
      "@stylistic/quotes": ["error", "double"],
      "@stylistic/semi": ["error", "always"],
      "@stylistic/type-annotation-spacing": "error",
      "@typescript-eslint/unified-signatures": "error",

      /* ---- JAVASCRIPT RULES ---- */
      "arrow-body-style": "error",
      "@stylistic/brace-style": ["error", "1tbs", { "allowSingleLine": true }],
      "constructor-super": "error",
      "curly": "error",
      "dot-notation": "off",
      "@stylistic/eol-last": "error",
      "eqeqeq": ["error", "smart"],
      "guard-for-in": "error",
      "@stylistic/max-len": ["error", { "code": 140 }],
      "no-bitwise": "error",
      "no-caller": "error",
      "no-console": [
        "error",
        {
          "allow": [
            "log",
            "warn",
            "dir",
            "timeLog",
            "assert",
            "clear",
            "count",
            "countReset",
            "group",
            "groupEnd",
            "table",
            "dirxml",
            "error",
            "groupCollapsed",
            "Console",
            "profile",
            "profileEnd",
            "timeStamp",
            "context",
            "createTask"
          ]
        }
      ],
      "no-debugger": "error",
      "no-eval": "error",
      "no-fallthrough": "error",
      "no-new-wrappers": "error",
      "no-restricted-imports": ["error", "rxjs/Rx"],
      "no-throw-literal": "error",
      "@stylistic/no-trailing-spaces": "error",
      "no-undef-init": "error",
      "no-unused-labels": "error",
      "no-var": "error",
      "prefer-const": "error",
      "radix": "error",
      "@stylistic/spaced-comment": [
        "error",
        "always",
        { "markers": ["/"] }
      ]
    }
  },
  {
    files: ["**/*.html"],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility]
  }
);
