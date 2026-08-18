-- V3__seed_role_permissions.sql

-- Map all permissions to the 'owner' role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'owner'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map management permissions to the 'admin' role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'admin'
  AND p.name IN ('users.read', 'users.invite')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map standard permissions to the 'member' role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'member'
  AND p.name IN ('projects.write')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map read-only permissions to the 'viewer' role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'viewer'
  AND p.name IN ('users.read', 'billing.read')
ON CONFLICT (role_id, permission_id) DO NOTHING;