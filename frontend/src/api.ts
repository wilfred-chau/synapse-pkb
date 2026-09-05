import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';

const ACCESS_TOKEN_STORAGE_KEY = 'synapse-pkb.access-token';
const REQUEST_ID_HEADER = 'X-Request-Id';

type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: {
    code: string;
    message: string;
    details: Record<string, unknown>;
  } | null;
  requestId: string | null;
  timestamp: string;
};

type RequestMetadata = {
  startedAt: number;
  requestId: string;
};

type RequestConfigWithMetadata = InternalAxiosRequestConfig & {
  metadata?: RequestMetadata;
};

export class ApiClientError extends Error {
  status: number | null;
  code: string | null;
  requestId: string | null;
  details: Record<string, unknown> | null;

  constructor(options: {
    message: string;
    status?: number | null;
    code?: string | null;
    requestId?: string | null;
    details?: Record<string, unknown> | null;
  }) {
    super(options.message);
    this.name = 'ApiClientError';
    this.status = options.status ?? null;
    this.code = options.code ?? null;
    this.requestId = options.requestId ?? null;
    this.details = options.details ?? null;
  }
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const requestConfig = config as RequestConfigWithMetadata;
  const accessToken = localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  const requestId = globalThis.crypto?.randomUUID?.() ?? `req-${Date.now()}`;

  if (accessToken) {
    requestConfig.headers.Authorization = `Bearer ${accessToken}`;
  }

  requestConfig.headers[REQUEST_ID_HEADER] = requestId;
  requestConfig.metadata = {
    startedAt: Date.now(),
    requestId
  };

  if (import.meta.env.DEV) {
    console.debug('[api] request', {
      method: requestConfig.method,
      url: requestConfig.url,
      requestId,
      data: requestConfig.data
    });
  }

  return requestConfig;
});

http.interceptors.response.use(
  (response) => {
    const requestConfig = response.config as RequestConfigWithMetadata;
    const durationMs = requestConfig.metadata ? Date.now() - requestConfig.metadata.startedAt : null;

    if (import.meta.env.DEV) {
      console.debug('[api] response', {
        method: response.config.method,
        url: response.config.url,
        status: response.status,
        requestId: response.data?.requestId ?? requestConfig.metadata?.requestId ?? null,
        durationMs,
        data: response.data
      });
    }

    return response;
  },
  (error: AxiosError<ApiEnvelope<never>>) => {
    const normalizedError = normalizeApiError(error);
    if (import.meta.env.DEV) {
      console.debug('[api] error', normalizedError);
    }
    return Promise.reject(normalizedError);
  }
);

function normalizeApiError(error: AxiosError<ApiEnvelope<never>>) {
  const responseBody = error.response?.data;
  const normalizedError = new ApiClientError({
    message: responseBody?.error?.message ?? error.message ?? '请求失败',
    status: error.response?.status ?? null,
    code: responseBody?.error?.code ?? null,
    requestId: responseBody?.requestId ?? error.config?.headers?.[REQUEST_ID_HEADER]?.toString() ?? null,
    details: responseBody?.error?.details ?? null
  });

  if (normalizedError.status === 401) {
    clearStoredAccessToken();
    window.dispatchEvent(new Event('auth:unauthorized'));
  }

  return normalizedError;
}

function unwrapApiEnvelope<T>(envelope: ApiEnvelope<T>) {
  if (envelope.success && envelope.data !== null) {
    return envelope.data;
  }

  throw new ApiClientError({
    message: envelope.error?.message ?? '请求失败',
    code: envelope.error?.code ?? null,
    requestId: envelope.requestId,
    details: envelope.error?.details ?? null
  });
}

export async function getApi<T>(url: string, config?: AxiosRequestConfig) {
  const response = await http.get<ApiEnvelope<T>>(url, config);
  return unwrapApiEnvelope(response.data);
}

export async function postApi<T, TBody = unknown>(url: string, body?: TBody, config?: AxiosRequestConfig) {
  const response = await http.post<ApiEnvelope<T>>(url, body, config);
  return unwrapApiEnvelope(response.data);
}

export function getStoredAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
}

export function storeAccessToken(accessToken: string) {
  localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken);
}

export function clearStoredAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
}
