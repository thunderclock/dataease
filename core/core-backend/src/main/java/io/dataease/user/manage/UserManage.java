package io.dataease.user.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.dataease.api.permissions.user.dto.*;
import io.dataease.api.permissions.user.vo.*;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.exception.DEException;
import io.dataease.model.KeywordRequest;
import io.dataease.user.dao.UserMapper;
import io.dataease.user.dao.UserRoleMapper;
import io.dataease.user.entity.User;
import io.dataease.user.entity.UserRole;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.CommonBeanFactory;
import io.dataease.utils.LogUtil;
import io.dataease.utils.Md5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理类
 */
@Component
public class UserManage {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public UserManage(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 分页查询用户列表
     */
    public IPage<UserGridVO> pager(int goPage, int pageSize, UserGridRequest request) {
        Page<User> page = new Page<>(goPage, pageSize);
        // 从当前用户获取组织ID，或者使用默认值
        Long orgId = getCurrentUserOrgId();
        IPage<User> userPage = userMapper.selectUserPage(page, request.getKeyword(), orgId);
        
        return userPage.convert(this::convertToUserGridVO);
    }

    /**
     * 根据ID查询用户详情
     */
    public UserFormVO queryById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            DEException.throwException("用户不存在");
        }
        return convertToUserFormVO(user);
    }

    /**
     * 查询个人信息
     */
    public UserFormVO personInfo() {
        TokenUserBO userBO = AuthUtils.getUser();
        if (userBO == null) {
            DEException.throwException("用户未登录");
        }
        return queryById(userBO.getUserId());
    }

    /**
     * 创建用户
     */
    public void create(UserCreator creator) {
        // 检查账号是否已存在
        if (StringUtils.isNotBlank(creator.getAccount())) {
            User existUser = userMapper.selectByAccount(creator.getAccount());
            if (existUser != null) {
                DEException.throwException("账号已存在");
            }
        }

        // 检查邮箱是否已存在
        if (StringUtils.isNotBlank(creator.getEmail())) {
            User existUser = userMapper.selectByEmail(creator.getEmail());
            if (existUser != null) {
                DEException.throwException("邮箱已存在");
            }
        }

        User user = new User();
        BeanUtils.copyProperties(creator, user);
        
        // 处理name字段映射到username
        if (StringUtils.isNotBlank(creator.getName())) {
            user.setUsername(creator.getName());
        }
        
        // 设置默认密码（如果前端没有传递密码）
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(Md5Utils.md5("123456")); // 默认密码
        } else {
            user.setPassword(Md5Utils.md5(user.getPassword())); // 加密密码
        }
        
        user.setCreateTime(System.currentTimeMillis());
        user.setUpdateTime(System.currentTimeMillis());
        user.setEnable(true);
        user.setDeleted(false);
        user.setLanguage("zh-CN");
        user.setOrgId(1L); // 设置默认组织ID

        TokenUserBO currentUser = AuthUtils.getUser();
        if (currentUser != null) {
            user.setCreator(currentUser.getUserId());
        }

        int result = userMapper.insert(user);
        if (result <= 0) {
            DEException.throwException("创建用户失败");
        }

        // 分配角色
        if (!CollectionUtils.isEmpty(creator.getRoleIds())) {
            assignRoles(user.getId(), creator.getRoleIds());
        }
    }

    /**
     * 编辑用户
     */
    public void edit(UserEditor editor) {
        User user = userMapper.selectById(editor.getId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 检查账号是否被其他用户使用
        if (StringUtils.isNotBlank(editor.getAccount()) && !editor.getAccount().equals(user.getAccount())) {
            User existUser = userMapper.selectByAccount(editor.getAccount());
            if (existUser != null && !existUser.getId().equals(editor.getId())) {
                DEException.throwException("账号已被其他用户使用");
            }
        }

        // 检查邮箱是否被其他用户使用
        if (StringUtils.isNotBlank(editor.getEmail()) && !editor.getEmail().equals(user.getEmail())) {
            User existUser = userMapper.selectByEmail(editor.getEmail());
            if (existUser != null && !existUser.getId().equals(editor.getId())) {
                DEException.throwException("邮箱已被其他用户使用");
            }
        }

        // 使用UpdateWrapper来选择性更新字段
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", editor.getId());
        
        // 更新基本信息
        if (StringUtils.isNotBlank(editor.getName())) {
            updateWrapper.set("username", editor.getName());
        }
        if (StringUtils.isNotBlank(editor.getAccount())) {
            updateWrapper.set("account", editor.getAccount());
        }
        if (StringUtils.isNotBlank(editor.getEmail())) {
            updateWrapper.set("email", editor.getEmail());
        }
        if (StringUtils.isNotBlank(editor.getPhone())) {
            updateWrapper.set("phone", editor.getPhone());
        }
        if (editor.getEnable() != null) {
            updateWrapper.set("enable", editor.getEnable());
        }
        
        // 处理密码更新：只有当密码不为空时才更新
        if (StringUtils.isNotBlank(editor.getPassword())) {
            updateWrapper.set("password", encryptPassword(editor.getPassword()));
        }
        
        updateWrapper.set("update_time", System.currentTimeMillis());

        int result = userMapper.update(null, updateWrapper);
        if (result <= 0) {
            DEException.throwException("更新用户失败");
        }

        // 更新角色
        if (!CollectionUtils.isEmpty(editor.getRoleIds())) {
            updateUserRoles(user.getId(), editor.getRoleIds());
        }
    }

    /**
     * 删除用户
     */
    public void delete(Long id) {
        if (id == 1L) {
            DEException.throwException("系统管理员不能删除");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        user.setDeleted(true);
        user.setUpdateTime(System.currentTimeMillis());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            DEException.throwException("删除用户失败");
        }
    }

    /**
     * 批量删除用户
     */
    public void batchDel(List<Long> ids) {
        if (ids.contains(1L)) {
            DEException.throwException("系统管理员不能删除");
        }

        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 切换用户状态
     */
    public void enable(EnableSwitchRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        user.setEnable(request.getEnable());
        user.setUpdateTime(System.currentTimeMillis());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            DEException.throwException("更新用户状态失败");
        }
    }

    /**
     * 重置密码
     */
    public void resetPwd(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 设置默认密码
        String defaultPwd = getDefaultPassword();
        user.setPassword(encryptPassword(defaultPwd));
        user.setUpdateTime(System.currentTimeMillis());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            DEException.throwException("重置密码失败");
        }
    }

    /**
     * 修改密码
     */
    public void modifyPwd(ModifyPwdRequest request) {
        TokenUserBO userBO = AuthUtils.getUser();
        if (userBO == null) {
            DEException.throwException("用户未登录");
        }

        User user = userMapper.selectById(userBO.getUserId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 验证旧密码
        if (!verifyPassword(request.getPwd(), user.getPassword())) {
            DEException.throwException("旧密码错误");
        }

        user.setPassword(encryptPassword(request.getNewPwd()));
        user.setUpdateTime(System.currentTimeMillis());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            DEException.throwException("修改密码失败");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    public CurUserVO info() {
        TokenUserBO userBO = AuthUtils.getUser();
        if (userBO == null) {
            DEException.throwException("用户未登录");
        }

        User user = userMapper.selectById(userBO.getUserId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        CurUserVO curUserVO = new CurUserVO();
        BeanUtils.copyProperties(user, curUserVO);
        return curUserVO;
    }

    /**
     * 根据账号查询用户
     */
    public CurUserVO queryByAccount(String account) {
        User user = userMapper.selectByAccount(account);
        if (user == null) {
            return null;
        }

        CurUserVO curUserVO = new CurUserVO();
        BeanUtils.copyProperties(user, curUserVO);
        return curUserVO;
    }

    /**
     * 查询当前组织内用户
     */
    public List<UserItem> byCurOrg(KeywordRequest request) {
        TokenUserBO userBO = AuthUtils.getUser();
        if (userBO == null) {
            return new ArrayList<>();
        }

        List<User> users = userMapper.selectByOrgId(userBO.getDefaultOid());
        return users.stream()
                .filter(user -> StringUtils.isBlank(request.getKeyword()) || 
                        user.getUsername().contains(request.getKeyword()) ||
                        user.getAccount().contains(request.getKeyword()))
                .map(this::convertToUserItem)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户数量
     */
    public int userCount() {
        return userMapper.countUsers(null);
    }

    /**
     * 获取默认密码
     */
    public String getDefaultPassword() {
        return "DataEase@123456";
    }

    /**
     * 更新最后登录信息
     */
    public void updateLastLogin(Long userId, String loginIp) {
        userMapper.updateLastLogin(userId, System.currentTimeMillis(), loginIp);
    }

    /**
     * 用户认证
     */
    public User authenticateUser(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            return null;
        }

        // 根据账号查询用户
        User user = userMapper.selectByAccount(account);
        if (user == null) {
            return null;
        }

        // 验证密码
        if (!verifyPassword(password, user.getPassword())) {
            return null;
        }

        return user;
    }

    /**
     * 分配角色
     */
    private void assignRoles(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        
        // 先删除用户的所有角色
        userRoleMapper.deleteByUserId(userId);
        
        // 去重角色ID列表
        List<Long> uniqueRoleIds = roleIds.stream().distinct().collect(Collectors.toList());
        
        // 批量插入新角色
        Long createTime = System.currentTimeMillis();
        userRoleMapper.batchInsert(userId, uniqueRoleIds, createTime);
        
        LogUtil.info("为用户 {} 分配角色: {}", userId, uniqueRoleIds);
    }

    /**
     * 更新用户角色
     */
    private void updateUserRoles(Long userId, List<Long> roleIds) {
        assignRoles(userId, roleIds);
    }

    /**
     * 加密密码
     */
    private String encryptPassword(String password) {
        // 使用MD5加密，实际生产环境建议使用BCrypt
        return Md5Utils.md5(password);
    }

    /**
     * 验证密码
     */
    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        // 使用MD5验证，实际生产环境建议使用BCrypt
        String encryptedRawPassword = Md5Utils.md5(rawPassword);
        return encryptedRawPassword.equals(encodedPassword);
    }

    /**
     * 转换为UserGridVO
     */
    private UserGridVO convertToUserGridVO(User user) {
        UserGridVO vo = new UserGridVO();
        BeanUtils.copyProperties(user, vo);
        
        // 设置name字段（使用username作为name）
        vo.setName(user.getUsername());
        
        // 查询用户角色
        List<Map<String, Object>> roles = userMapper.selectUserRoles(user.getId());
        List<UserGridRoleItem> roleItems = new ArrayList<>();
        for (Map<String, Object> role : roles) {
            UserGridRoleItem roleItem = new UserGridRoleItem();
            roleItem.setId((Long) role.get("role_id"));
            roleItem.setName((String) role.get("role_name"));
            roleItems.add(roleItem);
        }
        vo.setRoleItems(roleItems);
        
        return vo;
    }

    /**
     * 转换为UserFormVO
     */
    private UserFormVO convertToUserFormVO(User user) {
        UserFormVO vo = new UserFormVO();
        BeanUtils.copyProperties(user, vo);
        
        // 查询用户角色 - UserFormVO 使用 roleIds 字段，不是 roles
        // List<Map<String, Object>> roles = userMapper.selectUserRoles(user.getId());
        // vo.setRoleIds(roles); // 需要转换为 String 类型
        
        return vo;
    }

    /**
     * 转换为UserItem
     */
    private UserItem convertToUserItem(User user) {
        UserItem item = new UserItem();
        BeanUtils.copyProperties(user, item);
        return item;
    }

    /**
     * 获取当前用户组织ID
     */
    private Long getCurrentUserOrgId() {
        TokenUserBO userBO = AuthUtils.getUser();
        return userBO != null ? userBO.getDefaultOid() : 1L;
    }
}
