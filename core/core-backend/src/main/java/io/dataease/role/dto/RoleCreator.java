package io.dataease.role.dto;

import lombok.Data;

/**
 * 角色创建DTO
 */
@Data
public class RoleCreator {
    
    private String name;
    
    private String description;
    
    private Boolean enable;
}
