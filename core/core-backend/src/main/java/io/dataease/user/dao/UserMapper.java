package io.dataease.user.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.dataease.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 分页查询用户列表
     */
    IPage<User> selectUserPage(Page<User> page, @Param("keyword") String keyword, @Param("orgId") Long orgId);

    /**
     * 根据账号查询用户
     */
    User selectByAccount(@Param("account") String account);

    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 查询组织内用户
     */
    List<User> selectByOrgId(@Param("orgId") Long orgId);

    /**
     * 批量查询用户信息
     */
    List<Map<String, Object>> selectUserInfosByIds(@Param("ids") List<Long> ids);

    /**
     * 根据账号查询用户ID
     */
    Long selectUserIdByAccount(@Param("account") String account);

    /**
     * 根据用户名查询用户ID
     */
    List<Long> selectUserIdByName(@Param("name") String name);

    /**
     * 更新最后登录信息
     */
    int updateLastLogin(@Param("id") Long id, @Param("loginTime") Long loginTime, @Param("loginIp") String loginIp);

    /**
     * 统计用户数量
     */
    int countUsers(@Param("orgId") Long orgId);

    /**
     * 查询用户角色信息
     */
    List<Map<String, Object>> selectUserRoles(@Param("userId") Long userId);
}
