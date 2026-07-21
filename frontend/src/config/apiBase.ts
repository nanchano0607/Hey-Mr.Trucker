const normalizeBase = (base?: string) => {
  const b = (base ?? "").trim();
  if (!b) return "";
  return b.replace(/\/+$/, "");
};

/**
 * API/백엔드 베이스 URL
 * - 개발: 기본값 http://localhost:8080 (필요시 .env.local 에서 VITE_API_BASE_URL로 override)
 * - 운영: 기본값 "" (same-origin). Nginx에서 /api, /auth, /oauth2, /images, /logo 등 백엔드로 proxy 권장
 */
// //원래 코드 (운영용)
// export const API_BASE_URL = normalizeBase(
//   import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? "https://heymrtrucker.com" : "")
// );

// //로컬 테스트용
export const API_BASE_URL = normalizeBase(
  import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? "http://localhost:8080" : "")
);

export const withBase = (path: string) => {
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${p}`;
};
