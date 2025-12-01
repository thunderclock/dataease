package io.dataease.auth.api;

import io.dataease.auth.entity.AccessKey;
import io.dataease.auth.manage.AccessKeyManage;
import io.dataease.auth.vo.AccessKeyVO;
import io.dataease.user.dao.UserMapper;
import io.dataease.user.entity.User;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.LogUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AccessKey 管理 API
 */
@RestController
@RequestMapping("/accessKey")
public class AccessKeyApi {

    private final AccessKeyManage accessKeyManage;
    private final UserMapper userMapper;

    public AccessKeyApi(AccessKeyManage accessKeyManage, UserMapper userMapper) {
        this.accessKeyManage = accessKeyManage;
        this.userMapper = userMapper;
    }

    /**
     * 生成 AccessKey
     */
    @PostMapping("/generate")
    public AccessKeyVO generate(@RequestBody GenerateRequest request) {
        Long creator = AuthUtils.getUser().getUserId();
        Long userId = request.getUserId(); // 绑定的用户ID（必填）
        Long expireTime = request.getExpireTime(); // null 表示永不过期
        
        if (userId == null) {
            throw new IllegalArgumentException("userId is required. AccessKey must be bound to a user for permission control.");
        }
        
        AccessKey accessKey = accessKeyManage.generateAccessKey(
            request.getName(), 
            creator, 
            userId, // 绑定的用户ID
            expireTime
        );
        
        LogUtil.info("User " + creator + " generated AccessKey: " + accessKey.getAccessKey() + " bound to user: " + userId);
        
        // 查询用户信息用于显示
        User user = userMapper.selectById(userId);
        String userName = null;
        if (user != null) {
            userName = user.getUsername();
        }
        
        AccessKeyVO vo = new AccessKeyVO();
        vo.setId(accessKey.getId());
        vo.setAccessKey(accessKey.getAccessKey());
        vo.setAccessSecret(accessKey.getAccessSecret()); // 只在生成时返回一次
        vo.setName(accessKey.getName());
        vo.setCreator(accessKey.getCreator());
        vo.setUserId(accessKey.getUserId());
        vo.setUserName(userName);
        vo.setCreateTime(accessKey.getCreateTime());
        vo.setExpireTime(accessKey.getExpireTime());
        vo.setEnable(accessKey.getEnable());
        
        return vo;
    }

    /**
     * 查询 AccessKey 列表
     */
    @GetMapping("/list")
    public List<AccessKeyVO> list() {
        List<AccessKey> keys = accessKeyManage.listAll();
        return keys.stream()
                .map(key -> {
                    AccessKeyVO vo = new AccessKeyVO();
                    vo.setId(key.getId());
                    vo.setAccessKey(key.getAccessKey());
                    vo.setAccessSecret(null); // 查询时不返回 secret
                    vo.setName(key.getName());
                    vo.setCreator(key.getCreator());
                    vo.setUserId(key.getUserId());
                    
                    // 查询用户信息用于显示
                    if (key.getUserId() != null) {
                        try {
                            User user = userMapper.selectById(key.getUserId());
                            vo.setUserName(user != null ? user.getUsername() : null);
                        } catch (Exception e) {
                            LogUtil.warn("Failed to query user info for userId: " + key.getUserId(), e);
                            vo.setUserName(null);
                        }
                    }
                    
                    vo.setCreateTime(key.getCreateTime());
                    vo.setUpdateTime(key.getUpdateTime());
                    vo.setExpireTime(key.getExpireTime());
                    vo.setEnable(key.getEnable());
                    vo.setLastUseTime(key.getLastUseTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 禁用 AccessKey
     */
    @PostMapping("/disable/{id}")
    public void disable(@PathVariable Long id) {
        accessKeyManage.disableAccessKey(id);
        LogUtil.info("AccessKey disabled: " + id);
    }

    /**
     * 删除 AccessKey
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        accessKeyManage.deleteAccessKey(id);
        LogUtil.info("AccessKey deleted: " + id);
    }

    /**
     * 生成请求
     */
    public static class GenerateRequest {
        private String name;
        private Long userId; // 绑定的用户ID（必填，用于权限控制）
        private Long expireTime; // 过期时间戳，null 表示永不过期

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(Long expireTime) {
            this.expireTime = expireTime;
        }
    }
}

