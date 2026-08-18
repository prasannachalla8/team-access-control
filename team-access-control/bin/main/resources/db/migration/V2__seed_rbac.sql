CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Seed Permissions
INSERT INTO permissions (id, name) VALUES
    (uuid_generate_v4(), 'users.read'),
    (uuid_generate_v4(), 'users.invite'),
    (uuid_generate_v4(), 'projects.write'),
    (uuid_generate_v4(), 'billing.read'),
    (uuid_generate_v4(), 'audit.view');

-- Seed Roles
INSERT INTO roles (id, name, description) VALUES
    (uuid_generate_v4(), 'owner', 'Organization Owner with full access'),
    (uuid_generate_v4(), 'admin', 'Administrator with user and settings management'),
    (uuid_generate_v4(), 'member', 'Standard team member'),
    (uuid_generate_v4(), 'viewer', 'Read-only access');

-- Grant permissions to roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'owner';  -- owner gets everything

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'admin' AND p.name IN ('users.read', 'users.invite', 'billing.read', 'audit.view');