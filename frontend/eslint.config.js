// eslint.config.js  (ESM)
import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';

export default [
  // 무시 경로
  { ignores: ['dist', 'node_modules'] },

  // JS 기본 추천
  js.configs.recommended,

  // TS 추천 (flat config는 배열이라 전개)
  ...tseslint.configs.recommended,

  // 프로젝트 규칙/플러그인
  {
    files: ['**/*.{js,jsx,ts,tsx}'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
      parser: tseslint.parser,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: { jsx: true },
      },
    },
    plugins: {
      react,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    settings: { react: { version: 'detect' } },
    rules: {
      // React Hooks
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      // HMR-friendly
      'react-refresh/only-export-components': 'warn',
      // (선택) React 관련 기본 경고 줄이기
      'react/jsx-uses-react': 'off',    // 새 JSX 변환에선 불필요
      'react/react-in-jsx-scope': 'off' // React 17+
    },
  },
];
