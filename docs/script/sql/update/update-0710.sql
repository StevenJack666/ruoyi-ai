ALTER TABLE `knowledge_fragment` ADD COLUMN `vector` longtext NULL COMMENT '向量数据(逗号分隔浮点数)' AFTER `content';
