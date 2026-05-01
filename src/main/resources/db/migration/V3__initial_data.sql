-- ============================================================
-- V3 - Initial data
-- Admin password is "Admin@123!" (BCrypt encoded)
-- Change this immediately after first login!
-- ============================================================

INSERT INTO ROLES (NAME) VALUES ('ROLE_USER');
INSERT INTO ROLES (NAME) VALUES ('ROLE_ADMIN');

-- Admin user: username=admin, password=Admin@123!
INSERT INTO USERS (USERNAME, EMAIL, PASSWORD, ENABLED, ACCOUNT_NON_EXPIRED, ACCOUNT_NON_LOCKED, CREDENTIALS_NON_EXPIRED, MFA_ENABLED)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$12$g2dN3kfRpLXoUH0T3BXHXemVFQMVrKYh4iO6GVXQ4MTnT8CbX56hC',
    1, 1, 1, 1, 0
);

-- Assign ROLE_ADMIN to admin
INSERT INTO USER_ROLES (USER_ID, ROLE_ID)
SELECT U.ID, R.ID FROM USERS U, ROLES R
WHERE U.USERNAME = 'admin' AND R.NAME = 'ROLE_ADMIN';

-- Assign ROLE_USER to admin as well
INSERT INTO USER_ROLES (USER_ID, ROLE_ID)
SELECT U.ID, R.ID FROM USERS U, ROLES R
WHERE U.USERNAME = 'admin' AND R.NAME = 'ROLE_USER';
