package io.dataease.role.manage;

import io.dataease.api.permissions.role.dto.*;
import io.dataease.api.permissions.role.vo.*;
import io.dataease.model.KeywordRequest;
import io.dataease.role.entity.Role;
import io.dataease.role.dao.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

/**
 * 角色管理类
 */
@Component
public class RoleManage {

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 查询角色列表
     */
    public List<RoleVO> query(KeywordRequest request) {
        List<Role> roleList = roleMapper.selectList(null);
        List<RoleVO> roles = new ArrayList<>();
        
        for (Role role : roleList) {
            if (role.getDeleted() == null || !role.getDeleted()) {
                RoleVO roleVO = new RoleVO();
                roleVO.setId(role.getId());
                roleVO.setName(role.getName());
                roleVO.setReadonly(false);
                roleVO.setRoot(false);
                roles.add(roleVO);
            }
        }
        
        return roles;
    }

    /**
     * 查询角色详情
     */
    public RoleDetailVO detail(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return null;
        }
        
        RoleDetailVO detailVO = new RoleDetailVO();
        detailVO.setId(role.getId());
        detailVO.setName(role.getName());
        detailVO.setDesc(role.getDescription());
        detailVO.setTypeCode(1); // 默认类型
        
        return detailVO;
    }

    /**
     * 创建角色
     */
    public Long create(RoleCreator creator) {
        Role role = new Role();
        role.setName(creator.getName());
        role.setDescription(creator.getDesc());
        role.setType("CUSTOM");
        role.setCreateTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        role.setCreator(1L); // 默认创建者
        role.setDeleted(false);
        roleMapper.insert(role);
        return role.getId();
    }

    /**
     * 编辑角色
     */
    public void edit(RoleEditor editor) {
        Role role = new Role();
        role.setId(editor.getId());
        role.setName(editor.getName());
        role.setDescription(editor.getDesc());
        role.setUpdateTime(System.currentTimeMillis());
        roleMapper.updateById(role);
    }

    /**
     * 删除角色
     */
    public void delete(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setDeleted(true);
        role.setUpdateTime(System.currentTimeMillis());
        roleMapper.updateById(role);
    }

    /**
     * 角色可绑用户
     */
    public List<RoleVO> optionForUser(RoleRequest request) {
        // 简化实现
        return new ArrayList<>();
    }

    /**
     * 角色已绑用户
     */
    public List<RoleVO> selectedForUser(RoleRequest request) {
        // 简化实现
        return new ArrayList<>();
    }

    /**
     * 卸载用户
     */
    public void unMountUser(UnmountUserRequest request) {
        // 简化实现
    }

    /**
     * 绑定用户
     */
    public void mountUser(MountUserRequest request) {
        // 简化实现
    }

    /**
     * 卸载前信息
     */
    public Integer beforeUnmountInfo(UnmountUserRequest request) {
        // 简化实现
        return 0;
    }

    /**
     * 搜索外部用户
     */
    public ExternalUserVO searchExternalUser(String keyword) {
        // 简化实现
        return new ExternalUserVO();
    }

    /**
     * 绑定外部用户
     */
    public void mountExternalUser(MountExternalUserRequest request) {
        // 简化实现
    }

    /**
     * 复制角色
     */
    public void copy(RoleCopyRequest request) {
        // 简化实现
    }

    /**
     * 查询当前组织角色
     */
    public List<RoleVO> byCurOrg(KeywordRequest request) {
        // 简化实现
        return new ArrayList<>();
    }

    /**
     * 根据组织ID查询角色
     */
    public List<RoleVO> queryWithOid(Long oid) {
        // 简化实现
        return new ArrayList<>();
    }
}
