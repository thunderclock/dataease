package io.dataease.role.dto;

import lombok.Data;

/**
 * 角色编辑DTO
 */
@Data
public class RoleEditor {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Boolean enable;
}
