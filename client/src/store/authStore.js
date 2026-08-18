import { create } from "zustand";
import { decodeJwt } from "../utils/jwt";

function loadInitialUser() {
  const token = localStorage.getItem("accessToken");
  if (!token) return null;
  const claims = decodeJwt(token);
  if (!claims) return null;
  return { id: claims.sub, email: claims.email };
}

export const useAuthStore = create((set) => ({
  user: loadInitialUser(),
  isAuthenticated: !!localStorage.getItem("accessToken"),

  setTokens: ({ accessToken, refreshToken }) => {
    localStorage.setItem("accessToken", accessToken);
    if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
    const claims = decodeJwt(accessToken);
    set({
      isAuthenticated: true,
      user: claims ? { id: claims.sub, email: claims.email } : null,
    });
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    set({ user: null, isAuthenticated: false });
  },
}));