import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, ApiError, decodeToken, readToken, setUnauthorizedHandler, storeToken } from './api.js';

const AuthContext = createContext(null);

function validToken(token) {
  const claims = token && decodeToken(token);
  return claims && claims.exp * 1000 > Date.now() ? token : null;
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => validToken(readToken()));

  const logout = useCallback(() => {
    storeToken(null);
    setToken(null);
  }, []);

  // any 401 from a service means the token is no longer accepted
  useEffect(() => setUnauthorizedHandler(logout), [logout]);

  // sign out when the token expires
  useEffect(() => {
    if (!token) return undefined;
    const ms = decodeToken(token).exp * 1000 - Date.now();
    const timer = setTimeout(logout, Math.max(0, ms));
    return () => clearTimeout(timer);
  }, [token, logout]);

  const login = useCallback(async (email, password) => {
    const res = await api.login(email, password);
    // auth-service returns the token in "message"
    const next = validToken(res?.message);
    if (!next) throw new ApiError(500, 'Sign-in failed. Please try again.');
    storeToken(next);
    setToken(next);
  }, []);

  const value = useMemo(() => {
    const claims = token ? decodeToken(token) : null;
    return { token, user: claims ? { email: claims.sub, id: claims.userId } : null, login, logout };
  }, [token, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
