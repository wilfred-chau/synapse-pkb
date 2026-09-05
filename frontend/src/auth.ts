import { clearStoredAccessToken, getApi, getStoredAccessToken, postApi, storeAccessToken } from './api';

export type CurrentUser = {
  id: number;
  username: string;
  displayName: string;
  spaceKey: string;
};

type AuthResponse = {
  accessToken: string;
  user: CurrentUser;
};

export async function login(username: string, password: string) {
  const data = await postApi<AuthResponse, { username: string; password: string }>('/auth/login', { username, password });
  storeAccessToken(data.accessToken);
  return data.user;
}

export async function fetchCurrentUser() {
  return getApi<CurrentUser>('/auth/me');
}

export function hasStoredToken() {
  return Boolean(getStoredAccessToken());
}

export function logout() {
  clearStoredAccessToken();
}
