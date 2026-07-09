
-- 创建 access_grant 表
CREATE TABLE access_grant (
    id              TEXT        NOT NULL PRIMARY KEY,
    resource_type   TEXT        NOT NULL,
    resource_id     TEXT        NOT NULL,
    principal_type  TEXT        NOT NULL,
    principal_id    TEXT        NOT NULL,
    permission      TEXT        NOT NULL,
    created_at      BIGINT      NOT NULL,

    -- 五列联合唯一，防止重复授权
    CONSTRAINT uq_access_grant_grant
        UNIQUE (resource_type, resource_id, principal_type, principal_id, permission)
);

-- 索引：按资源查询（查某个工具/模型/知识库的所有授权）
CREATE INDEX idx_access_grant_resource
    ON access_grant (resource_type, resource_id);

-- 索引：按主体查询（查某个用户/用户组被授权了哪些资源）
CREATE INDEX idx_access_grant_principal
    ON access_grant (principal_type, principal_id);









CREATE TABLE skill (
    id          TEXT        NOT NULL PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    name        TEXT        NOT NULL UNIQUE,
    description TEXT        NULL,
    content     TEXT        NOT NULL,
    meta        JSON        NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT 1,
    updated_at  BIGINT      NOT NULL,
    created_at  BIGINT      NOT NULL
);

CREATE INDEX idx_skill_user_id ON skill (user_id);
CREATE INDEX idx_skill_updated_at ON skill (updated_at);

