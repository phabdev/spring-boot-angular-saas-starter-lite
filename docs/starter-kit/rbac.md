# Roles and permissions

RBAC means role-based access control. USER and ADMIN are the stored roles. Both can manage their own Projects; a guessed ID owned by someone else returns 404. The server derives identity from a verified token and current user data, never a request-body owner ID.

Pro exposes `/api/permissions` so the client can describe available operations. USER has `projects:read`, `projects:write`, `settings:write`, `billing:read`; ADMIN adds `users:manage` and `audit:read`. Admin user management and audit endpoints enforce ADMIN on the server. Hiding a menu or route is not authorization.

The roles editor changes an existing user's role. Registration never creates admins. Self-demotion is rejected to avoid accidentally removing the current admin's access. Bootstrap a local admin using explicit seed credentials as described in [installation](installation.md).

This is a deliberately small fixed role/permission model. Organization memberships, arbitrary custom roles, delegated ownership and permission editors are not included. Add them as a separate domain model with denial tests before claiming multi-tenancy or enterprise authorization.
