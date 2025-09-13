package io.dataease.user.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.dataease.user.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    
    /**
     * 删除用户的所有角色
     */
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 批量插入用户角色
     */
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds, @Param("createTime") Long createTime);
}
