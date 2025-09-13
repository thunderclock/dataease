package io.dataease.role.vo;

import lombok.Data;

/**
 * 角色项VO
 */
@Data
public class RoleItemVO {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Boolean enable;
}
