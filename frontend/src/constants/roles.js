export const ROLE = Object.freeze({
  ADMIN: 'admin',
  ANALYST: 'analyst',
  VIEWER: 'viewer',
});

export const ROLE_LABELS = {
  [ROLE.ADMIN]: 'Administrator',
  [ROLE.ANALYST]: 'Security Analyst',
  [ROLE.VIEWER]: 'Viewer',
};

export const AVAILABLE_ROLES = Object.values(ROLE);

export const ROLE_PERMISSIONS = {
  [ROLE.ADMIN]: ['dashboard', 'scans', 'reports', 'notifications', 'apikeys', 'users', 'settings'],
  [ROLE.ANALYST]: ['dashboard', 'scans', 'reports', 'notifications', 'settings'],
  [ROLE.VIEWER]: ['dashboard', 'reports', 'notifications', 'settings'],
};

export const ensureValidRole = (role) => {
  if (!role) return ROLE.VIEWER;
  const normalized = String(role).trim().toLowerCase();
  return AVAILABLE_ROLES.includes(normalized) ? normalized : ROLE.VIEWER;
};

