// axios.ts
import axios, { AxiosError } from "axios";
import type {
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosRequestHeaders,
} from "axios";
import { getAccessToken, setAccessToken, clearAccessToken } from "./token";
import { API_BASE_URL } from "../config/apiBase";

/**
 * 단일 axios 인스턴스
 * - 모든 API 호출부에서 이 인스턴스를 import 해서 사용하세요.
 * - baseURL이 필요하면 아래 주석 해제 후 환경에 맞게 설정.
 */
const api = axios.create({
  withCredentials: true,
  baseURL: API_BASE_URL || undefined,
});

/** 재발급 중인지 여부 (동시 401 시 stampede 방지) */
let isRefreshing = false;
/** 재발급 완료를 기다리는 대기열 */
let waitQueue: Array<(token: string | null) => void> = [];

/** 대기중인 요청들에게 재발급 결과 브로드캐스트 */
function broadcast(token: string | null) {
  waitQueue.forEach((resolve) => resolve(token));
  waitQueue = [];
}

/** /api/token 호출용 "깨끗한" axios (인터셉터 미적용) */
const plain = axios.create({ withCredentials: true, baseURL: API_BASE_URL || undefined });

/** 액세스 토큰 재발급 */
async function refreshAccessToken(): Promise<string | null> {
  try {
    const res = await plain.post(
      `/api/token`,
      {},
      { validateStatus: (s) => s >= 200 && s < 300 }
    );
    const newToken = res.data?.accessToken as string | undefined;
    if (newToken) {
      setAccessToken(newToken);
      return newToken;
    }
    clearAccessToken();
    return null;
  } catch {
    clearAccessToken();
    return null;
  }
}

/** 요청 인터셉터: Authorization 자동 첨부 */
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // /api/token 호출에는 Authorization 붙이지 않음(루프/충돌 방지)
  const isTokenEndpoint = config.url?.includes("/api/token");
  if (!isTokenEndpoint) {
    const t = getAccessToken();
    if (t) {
      // headers는 타입이 union이라 얕은 복사 후 캐스팅이 안전함
      const headers: AxiosRequestHeaders =
        (config.headers as AxiosRequestHeaders) ?? {};
      headers.Authorization = `Bearer ${t}`;
      config.headers = headers;
    }
  }
  return config;
});

/** 응답 인터셉터: 401 처리(단일 비행 + 재시도 1회) */
api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { __retry?: boolean })
      | undefined;
    const status = error.response?.status ?? 0;

    if (!original || status !== 401 || original.__retry) {
      throw error;
    }

    // /api/token 자체가 401이면 더 시도하지 않음
    if (original.url?.includes("/api/token")) {
      clearAccessToken();
      throw error;
    }

    // 🔹 여기 추가: "애초에 로그인 요청이 아니었던 401" 은 리프레시 시도 안 함
    const headers = (original.headers as AxiosRequestHeaders) ?? {};
    const hadAuthHeader = !!headers.Authorization;
    const hadToken = !!getAccessToken();

    if (!hadAuthHeader && !hadToken) {
      // => 이건 그냥 비로그인/게스트의 401이다. /api/token 치지 말고 그대로 에러 반환.
      throw error;
    }

    // ===== 여기서부터 진짜 "로그인했던 유저의 401"만 처리 =====
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const newToken = await refreshAccessToken();
        broadcast(newToken);
      } finally {
        isRefreshing = false;
      }
    } else {
      const token = await new Promise<string | null>((resolve) =>
        waitQueue.push(resolve)
      );
      if (token == null) {
        clearAccessToken();
        throw error;
      }
    }

    const token = getAccessToken();
    original.__retry = true;

    const newHeaders: AxiosRequestHeaders =
      (original.headers as AxiosRequestHeaders) ?? {};
    if (token) {
      newHeaders.Authorization = `Bearer ${token}`;
    } else {
      delete (newHeaders as any).Authorization;
    }
    original.headers = newHeaders;

    return api.request(original as AxiosRequestConfig);
  }
);

export default api;
