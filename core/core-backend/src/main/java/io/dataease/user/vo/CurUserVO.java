package io.dataease.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户VO
 */
@Data
public class CurUserVO {
    
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
     * 语言设置
     */
    private String language;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 备注
     */
    private String remark;
}
