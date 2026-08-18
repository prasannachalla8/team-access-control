-- V4__add_role_management_permissions.sql

-- These two permissions back the role-change and remove-member endpoints,
-- added after the original permission set — they didn't exist when V3 ran,
-- so 'owner' never picked them up despite V3's "grant everything" intent.
INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'roles.assign'),
    (gen_random_uuid(), 'users.remove')
ON CONFLICT (name) DO NOTHING;

-- Grant to owner (owner should always have every permission)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'owner'
  AND p.name IN ('roles.assign', 'users.remove')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Grant to admin (matches your V3 pattern of giving admin management permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'admin'
  AND p.name IN ('roles.assign', 'users.remove')
ON CONFLICT (role_id, permission_id) DO NOTHING;