package io.dataease.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户编辑DTO
 */
@Data
public class UserEditor {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 显示名称
     */
    private String name;
    
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
    
    /**
     * 角色ID列表（字符串类型，用于前端传递）
     */
    private List<String> roleIdsStr;
}
