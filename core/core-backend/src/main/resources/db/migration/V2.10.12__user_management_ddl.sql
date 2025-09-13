-- DataEase 用户管理功能数据库初始化脚本
-- 版本: V2.10.12
-- 创建时间: 2024年
-- 说明: 为用户管理功能创建必要的数据库表结构

-- 用户表
CREATE TABLE IF NOT EXISTS `core_user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `account` VARCHAR(100) NOT NULL COMMENT '账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `enable` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    `org_id` BIGINT(20) DEFAULT NULL COMMENT '组织ID',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `creator` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言设置',
    `last_login_time` BIGINT DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像',
    `remark` TEXT DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除 1-已删除 0-未删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account` (`account`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_enable` (`enable`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `core_role` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    `type` VARCHAR(20) DEFAULT 'CUSTOM' COMMENT '角色类型 SYSTEM-系统角色 CUSTOM-自定义角色',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `creator` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除 1-已删除 0-未删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_type` (`type`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `core_user_role` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `core_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `core_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 组织表
CREATE TABLE IF NOT EXISTS `core_organization` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '组织ID',
    `name` VARCHAR(100) NOT NULL COMMENT '组织名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '组织描述',
    `parent_id` BIGINT(20) DEFAULT NULL COMMENT '父组织ID',
    `level` INT(11) DEFAULT 1 COMMENT '组织层级',
    `sort` INT(11) DEFAULT 0 COMMENT '排序',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `creator` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除 1-已删除 0-未删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织表';

-- 权限表
CREATE TABLE IF NOT EXISTS `core_permission` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `type` VARCHAR(20) NOT NULL COMMENT '权限类型 MENU-菜单权限 BUTTON-按钮权限 API-接口权限',
    `resource_id` VARCHAR(100) DEFAULT NULL COMMENT '资源ID',
    `parent_id` BIGINT(20) DEFAULT NULL COMMENT '父权限ID',
    `level` INT(11) DEFAULT 1 COMMENT '权限层级',
    `sort` INT(11) DEFAULT 0 COMMENT '排序',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除 1-已删除 0-未删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type` (`type`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `core_role_permission` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`),
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `core_role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `core_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 插入默认数据

-- 插入默认组织
INSERT INTO `core_organization` (`id`, `name`, `description`, `parent_id`, `level`, `sort`, `creator`) 
VALUES (1, '默认组织', '系统默认组织', NULL, 1, 0, 1) 
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 插入默认角色
INSERT INTO `core_role` (`id`, `name`, `description`, `type`, `creator`) 
VALUES 
(1, '系统管理员', '系统管理员角色，拥有所有权限', 'SYSTEM', 1),
(2, '普通用户', '普通用户角色，拥有基础权限', 'SYSTEM', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 插入默认用户（admin）
INSERT INTO `core_user` (`id`, `username`, `account`, `password`, `email`, `phone`, `enable`, `org_id`, `creator`, `language`) 
VALUES (1, '系统管理员', 'admin', MD5('DataEase@123456'), 'admin@dataease.io', '13800138000', 1, 1, 1, 'zh-CN')
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- 插入测试用户
INSERT INTO `core_user` (`id`, `username`, `account`, `password`, `email`, `phone`, `enable`, `org_id`, `creator`, `language`) 
VALUES 
(2, '测试用户1', 'test1', MD5('123456'), 'test1@dataease.io', '13800138001', 1, 1, 1, 'zh-CN'),
(3, '测试用户2', 'test2', MD5('123456'), 'test2@dataease.io', '13800138002', 1, 1, 1, 'zh-CN'),
(4, '禁用用户', 'disabled', MD5('123456'), 'disabled@dataease.io', '13800138003', 0, 1, 1, 'zh-CN')
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- 为admin用户分配系统管理员角色
INSERT INTO `core_user_role` (`user_id`, `role_id`) 
VALUES (1, 1)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- 为测试用户分配普通用户角色
INSERT INTO `core_user_role` (`user_id`, `role_id`) 
VALUES 
(2, 2),
(3, 2),
(4, 2)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- 插入基础权限
INSERT INTO `core_permission` (`id`, `name`, `code`, `type`, `resource_id`, `parent_id`, `level`, `sort`) 
VALUES 
(1, '用户管理', 'user:manage', 'MENU', 'user', NULL, 1, 1),
(2, '用户查看', 'user:read', 'BUTTON', 'user:read', 1, 2, 1),
(3, '用户创建', 'user:create', 'BUTTON', 'user:create', 1, 2, 2),
(4, '用户编辑', 'user:edit', 'BUTTON', 'user:edit', 1, 2, 3),
(5, '用户删除', 'user:delete', 'BUTTON', 'user:delete', 1, 2, 4),
(6, '角色管理', 'role:manage', 'MENU', 'role', NULL, 1, 2),
(7, '角色查看', 'role:read', 'BUTTON', 'role:read', 6, 2, 1),
(8, '角色创建', 'role:create', 'BUTTON', 'role:create', 6, 2, 2),
(9, '角色编辑', 'role:edit', 'BUTTON', 'role:edit', 6, 2, 3),
(10, '角色删除', 'role:delete', 'BUTTON', 'role:delete', 6, 2, 4)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 为系统管理员角色分配所有权限
INSERT INTO `core_role_permission` (`role_id`, `permission_id`) 
SELECT 1, id FROM `core_permission` WHERE deleted = 0
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 为普通用户角色分配基础权限
INSERT INTO `core_role_permission` (`role_id`, `permission_id`) 
VALUES (2, 1), (2, 2), (2, 6), (2, 7)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 插入用户管理菜单
-- 用户管理菜单（放在数据准备后面，sort=5）
INSERT INTO `core_menu` (`id`, `pid`, `type`, `name`, `component`, `menu_sort`, `icon`, `path`, `hidden`, `in_layout`, `auth`)
VALUES (71, 0, 2, 'user-management', 'system/user-management', 5, 'peoples', '/user-management', 0, 1, 1)
ON DUPLICATE KEY UPDATE `menu_sort` = VALUES(`menu_sort`), `component` = VALUES(`component`), `type` = VALUES(`type`);

-- 删除可能存在的子菜单记录
DELETE FROM `core_menu` WHERE `id` = 72;

-- 创建索引优化查询性能
-- CREATE INDEX `idx_user_org_enable` ON `core_user` (`org_id`, `enable`, `deleted`);
-- CREATE INDEX `idx_user_create_time` ON `core_user` (`create_time` DESC);
-- CREATE INDEX `idx_role_type_deleted` ON `core_role` (`type`, `deleted`);
-- CREATE INDEX `idx_permission_type_deleted` ON `core_permission` (`type`, `deleted`);
