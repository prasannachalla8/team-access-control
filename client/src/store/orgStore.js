import { create } from "zustand";

// No longer persisted to localStorage — the whole point of this rewrite is
// that org membership must come from the backend (GET /organizations),
// not from whatever was last saved in this browser.
export const useOrgStore = create((set) => ({
  organizations: [],
  currentOrgId: null,
  loaded: false,

  setOrganizations: (orgs) =>
    set((state) => ({
      organizations: orgs,
      loaded: true,
      currentOrgId: orgs.some((o) => o.id === state.currentOrgId)
        ? state.currentOrgId
        : orgs[0]?.id || null,
    })),

  setCurrentOrgId: (id) => set({ currentOrgId: id }),

  addOrganization: (org) =>
    set((state) => ({
      organizations: [...state.organizations, org],
      currentOrgId: org.id,
    })),

  reset: () => set({ organizations: [], currentOrgId: null, loaded: false }),
}));

export function useCurrentOrg() {
  return useOrgStore((s) => s.organizations.find((o) => o.id === s.currentOrgId) || null);
}