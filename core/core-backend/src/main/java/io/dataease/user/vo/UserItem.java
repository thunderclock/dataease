package io.dataease.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户项VO
 */
@Data
public class UserItem {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 账号
     */
    private String account;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 是否启用
     */
    private Boolean enable;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 头像
     */
    private String avatar;
}
