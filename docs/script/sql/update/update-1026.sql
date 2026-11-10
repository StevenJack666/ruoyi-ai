ALTER TABLE session_upload_record
    ADD COLUMN parse_status TINYINT NOT NULL DEFAULT 0 COMMENT '解析进度：0-解析中，1-已完成，2-失败';
