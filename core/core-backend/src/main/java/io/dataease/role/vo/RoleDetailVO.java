package io.dataease.role.vo;

import lombok.Data;

/**
 * 角色详情VO
 */
@Data
public class RoleDetailVO {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Boolean enable;
    
    private Long createTime;
    
    private Long updateTime;
    
    private String createBy;
    
    private String updateBy;
}
