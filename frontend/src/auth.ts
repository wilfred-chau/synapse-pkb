import { http, clearStoredAccessToken, getStoredAccessToken, storeAccessToken } from './api';

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
  const { data } = await http.post<AuthResponse>('/auth/login', { username, password });
  storeAccessToken(data.accessToken);
  return data.user;
}

export async function fetchCurrentUser() {
  const { data } = await http.get<CurrentUser>('/auth/me');
  return data;
}

export function hasStoredToken() {
  return Boolean(getStoredAccessToken());
}

export function logout() {
  clearStoredAccessToken();
}
