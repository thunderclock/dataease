-- ----------------------------
-- Table structure for core_access_key
-- ----------------------------
DROP TABLE IF EXISTS `core_access_key`;
CREATE TABLE `core_access_key` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `access_key` varchar(255) NOT NULL COMMENT 'AccessKey',
    `access_secret` varchar(255) NOT NULL COMMENT 'AccessSecret',
    `name` varchar(255) DEFAULT NULL COMMENT '名称/描述',
    `creator` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` bigint NOT NULL COMMENT '创建时间',
    `update_time` bigint DEFAULT NULL COMMENT '更新时间',
    `expire_time` bigint DEFAULT NULL COMMENT '过期时间（时间戳，null 表示永不过期）',
    `enable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    `last_use_time` bigint DEFAULT NULL COMMENT '最后使用时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_access_key` (`access_key`),
    KEY `idx_creator` (`creator`),
    KEY `idx_enable` (`enable`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AccessKey表';

