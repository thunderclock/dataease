package io.dataease.auth.manage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.dataease.auth.dao.AccessTokenMapper;
import io.dataease.auth.entity.AccessToken;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import io.dataease.utils.LogUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * AccessToken 管理类
 */
@Component
public class AccessTokenManage {

    @Resource
    private AccessTokenMapper accessTokenMapper;

    /**
     * 生成 AccessToken 和 AccessSecret
     * 
     * @param name 名称/描述
     * @param creator 创建人ID
     * @param expireTime 过期时间（时间戳，null 表示永不过期）
     * @return AccessToken 对象
     */
    public AccessToken generateAccessToken(String name, Long creator, Long expireTime) {
        AccessToken accessToken = new AccessToken();
        
        // 生成 AccessToken（32 位随机字符串）
        String token = "de_" + RandomStringUtils.randomAlphanumeric(30);
        
        // 生成 AccessSecret（64 位随机字符串）
        String secret = RandomStringUtils.randomAlphanumeric(64);
        
        accessToken.setAccessToken(token);
        accessToken.setAccessSecret(secret);
        accessToken.setName(name);
        accessToken.setCreator(creator);
        accessToken.setCreateTime(System.currentTimeMillis());
        accessToken.setUpdateTime(System.currentTimeMillis());
        accessToken.setExpireTime(expireTime);
        accessToken.setEnable(true);
        
        accessTokenMapper.insert(accessToken);
        
        LogUtil.info("Generated AccessToken: " + token + " for creator: " + creator);
        
        return accessToken;
    }

    /**
     * 根据 AccessToken 查询 AccessSecret
     * 
     * @param token AccessToken
     * @return AccessToken 对象（包含 AccessSecret）
     */
    public AccessToken getByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        
        LambdaQueryWrapper<AccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessToken::getAccessToken, token);
        wrapper.eq(AccessToken::getEnable, true);
        
        AccessToken accessToken = accessTokenMapper.selectOne(wrapper);
        
        // 检查是否过期
        if (accessToken != null && accessToken.getExpireTime() != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > accessToken.getExpireTime()) {
                LogUtil.warn("AccessToken expired: " + token);
                return null;
            }
        }
        
        return accessToken;
    }

    /**
     * 更新最后使用时间
     * 
     * @param token AccessToken
     */
    public void updateLastUseTime(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        
        LambdaQueryWrapper<AccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessToken::getAccessToken, token);
        AccessToken accessToken = accessTokenMapper.selectOne(wrapper);
        
        if (accessToken != null) {
            accessToken.setLastUseTime(System.currentTimeMillis());
            accessTokenMapper.updateById(accessToken);
        }
    }

    /**
     * 禁用 AccessToken
     * 
     * @param id AccessToken ID
     */
    public void disableAccessToken(Long id) {
        AccessToken accessToken = accessTokenMapper.selectById(id);
        if (accessToken != null) {
            accessToken.setEnable(false);
            accessToken.setUpdateTime(System.currentTimeMillis());
            accessTokenMapper.updateById(accessToken);
        }
    }

    /**
     * 删除 AccessToken
     * 
     * @param id AccessToken ID
     */
    public void deleteAccessToken(Long id) {
        accessTokenMapper.deleteById(id);
    }

    /**
     * 查询所有 AccessToken
     * 
     * @return AccessToken 列表
     */
    public List<AccessToken> listAll() {
        return accessTokenMapper.selectList(new LambdaQueryWrapper<>());
    }
}

