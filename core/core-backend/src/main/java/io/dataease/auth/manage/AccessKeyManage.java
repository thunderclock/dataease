package io.dataease.auth.manage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.dataease.auth.dao.AccessKeyMapper;
import io.dataease.auth.entity.AccessKey;
import io.dataease.utils.LogUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AccessKey 管理类
 */
@Component
public class AccessKeyManage {

    @Resource
    private AccessKeyMapper accessKeyMapper;

    /**
     * 生成 AccessKey 和 AccessSecret
     * 
     * @param name 名称/描述
     * @param creator 创建人ID
     * @param userId 绑定的用户ID（用于权限控制，必填）
     * @param expireTime 过期时间（时间戳，null 表示永不过期）
     * @return AccessKey 对象
     */
    public AccessKey generateAccessKey(String name, Long creator, Long userId, Long expireTime) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null. AccessKey must be bound to a user for permission control.");
        }
        
        AccessKey accessKey = new AccessKey();
        
        // 生成 AccessKey（32 位随机字符串）
        String key = "de_" + RandomStringUtils.randomAlphanumeric(30);
        
        // 生成 AccessSecret（64 位随机字符串）
        String secret = RandomStringUtils.randomAlphanumeric(64);
        
        accessKey.setAccessKey(key);
        accessKey.setAccessSecret(secret);
        accessKey.setName(name);
        accessKey.setCreator(creator);
        accessKey.setUserId(userId); // 设置绑定的用户ID
        accessKey.setCreateTime(System.currentTimeMillis());
        accessKey.setUpdateTime(System.currentTimeMillis());
        accessKey.setExpireTime(expireTime);
        accessKey.setEnable(true);
        
        accessKeyMapper.insert(accessKey);
        
        LogUtil.info("Generated AccessKey: " + key + " for creator: " + creator + ", bound to user: " + userId);
        
        return accessKey;
    }

    /**
     * 根据 AccessKey 查询 AccessSecret
     * 
     * @param key AccessKey
     * @return AccessKey 对象（包含 AccessSecret）
     */
    public AccessKey getByKey(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        
        LambdaQueryWrapper<AccessKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessKey::getAccessKey, key);
        wrapper.eq(AccessKey::getEnable, true);
        
        AccessKey accessKey = accessKeyMapper.selectOne(wrapper);
        
        // 检查是否过期
        if (accessKey != null && accessKey.getExpireTime() != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > accessKey.getExpireTime()) {
                LogUtil.warn("AccessKey expired: " + key);
                return null;
            }
        }
        
        return accessKey;
    }

    /**
     * 更新最后使用时间
     * 
     * @param key AccessKey
     */
    public void updateLastUseTime(String key) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        
        LambdaQueryWrapper<AccessKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessKey::getAccessKey, key);
        AccessKey accessKey = accessKeyMapper.selectOne(wrapper);
        
        if (accessKey != null) {
            accessKey.setLastUseTime(System.currentTimeMillis());
            accessKeyMapper.updateById(accessKey);
        }
    }

    /**
     * 禁用 AccessKey
     * 
     * @param id AccessKey ID
     */
    public void disableAccessKey(Long id) {
        AccessKey accessKey = accessKeyMapper.selectById(id);
        if (accessKey != null) {
            accessKey.setEnable(false);
            accessKey.setUpdateTime(System.currentTimeMillis());
            accessKeyMapper.updateById(accessKey);
        }
    }

    /**
     * 删除 AccessKey
     * 
     * @param id AccessKey ID
     */
    public void deleteAccessKey(Long id) {
        accessKeyMapper.deleteById(id);
    }

    /**
     * 查询所有 AccessKey
     * 
     * @return AccessKey 列表
     */
    public List<AccessKey> listAll() {
        return accessKeyMapper.selectList(new LambdaQueryWrapper<>());
    }
}

