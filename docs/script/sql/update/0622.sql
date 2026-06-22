CREATE TABLE session_upload_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT '上传用户ID',
    session_id   BIGINT       NOT NULL COMMENT '会话ID',
    file_name    VARCHAR(500) NOT NULL COMMENT '文件名',
    file_type    VARCHAR(20)  NOT NULL COMMENT '文件扩展名',
    file_size    BIGINT       NOT NULL COMMENT '文件大小(字节)',
    oss_id       BIGINT       NOT NULL COMMENT 'OSS存储ID',
    chunk_count  INT          DEFAULT 0 COMMENT '分块数量',
    upload_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_user (user_id)
);