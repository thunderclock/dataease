-- ----------------------------
-- Add user_id column to core_access_key table
-- AccessKey 强制绑定用户，支持行级权限
-- ----------------------------
ALTER TABLE `core_access_key` 
ADD COLUMN `user_id` bigint NOT NULL COMMENT '绑定的用户ID（用于权限控制）' AFTER `creator`,
ADD KEY `idx_user_id` (`user_id`);

-- 将现有记录的 user_id 设置为 creator（兼容旧数据）
UPDATE `core_access_key` SET `user_id` = `creator` WHERE `user_id` IS NULL OR `user_id` = 0;

