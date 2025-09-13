package io.dataease.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体类
 */
@Data
@TableName("core_role")
public class Role {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String type;
    
    private Long createTime;
    
    private Long updateTime;
    
    private Long creator;
    
    private Boolean deleted;
}
