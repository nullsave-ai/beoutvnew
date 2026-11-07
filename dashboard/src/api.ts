const BASE = (import.meta.env.VITE_API as string | undefined) || "";
const TOKEN_KEY = "beout_token";
const ROLE_KEY = "beout_role";

export const session = {
  get token() { return sessionStorage.getItem(TOKEN_KEY); },
  get role() { return sessionStorage.getItem(ROLE_KEY); },
  set(t: string, r: string) { sessionStorage.setItem(TOKEN_KEY, t); sessionStorage.setItem(ROLE_KEY, r); },
  clear() { sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(ROLE_KEY); },
};

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

let onUnauthorized: () => void = () => {};
export const setUnauthorizedHandler = (f: () => void) => { onUnauthorized = f; };

export async function api<T = unknown>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}/v1/admin${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (res.status === 401 && path !== "/login") { session.clear(); onUnauthorized(); }
  if (!res.ok) {
    let msg = res.statusText;
    try { msg = (await res.json()).error ?? msg; } catch { /* ignore */ }
    throw new ApiError(res.status, msg);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const get = <T,>(p: string) => api<T>("GET", p);
export const post = <T,>(p: string, b?: unknown) => api<T>("POST", p, b ?? {});
export const put = <T,>(p: string, b?: unknown) => api<T>("PUT", p, b ?? {});
export const del = <T,>(p: string) => api<T>("DELETE", p);
