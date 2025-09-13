package io.dataease.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户列表展示VO
 */
@Data
public class UserGridVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 姓名（前端显示用）
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
     * 创建时间
     */
    private LocalDateTime createTime;
    
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
    
    /**
     * 角色列表
     */
    private List<Map<String, Object>> roles;
    
    /**
     * 角色项列表（前端显示用）
     */
    private List<Map<String, Object>> roleItems;
}
