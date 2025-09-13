package io.dataease.role.dto;

import lombok.Data;

/**
 * 角色请求DTO
 */
@Data
public class RoleRequest {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Boolean enable;
}
