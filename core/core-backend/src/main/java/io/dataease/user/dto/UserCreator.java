package io.dataease.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户创建DTO
 */
@Data
public class UserCreator {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 账号
     */
    private String account;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
    /**
     * 语言设置
     */
    private String language;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 角色ID列表
     */
    private List<Long> roleIds;
}
