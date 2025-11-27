-- ----------------------------
-- AccessKey 管理菜单配置
-- ----------------------------

-- 插入 AccessKey 管理菜单（放在用户管理后面，sort=6）
INSERT INTO `core_menu` (`id`, `pid`, `type`, `name`, `component`, `menu_sort`, `icon`, `path`, `hidden`, `in_layout`, `auth`)
VALUES (72, 0, 2, 'access-key-management', 'system/access-key', 6, 'key', '/access-key-management', 0, 1, 1)
ON DUPLICATE KEY UPDATE 
  `menu_sort` = VALUES(`menu_sort`), 
  `component` = VALUES(`component`), 
  `type` = VALUES(`type`),
  `icon` = VALUES(`icon`),
  `path` = VALUES(`path`);

