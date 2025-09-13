package io.dataease.user.dto;

import lombok.Data;

/**
 * 用户列表查询请求DTO
 */
@Data
public class UserGridRequest {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
    /**
     * 是否启用
     */
    private Boolean enable;
}
