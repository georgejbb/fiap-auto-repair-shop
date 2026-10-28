-- Usuários com acesso às APIs administrativas. A senha é armazenada apenas como hash bcrypt.
CREATE TABLE admin_user (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_admin_user_username UNIQUE (username)
);
