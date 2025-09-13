package io.dataease.role.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.dataease.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
