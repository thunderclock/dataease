package io.dataease.auth.api;

import io.dataease.auth.entity.AccessToken;
import io.dataease.auth.manage.AccessTokenManage;
import io.dataease.auth.vo.AccessTokenVO;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.LogUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AccessToken 管理 API
 */
@RestController
@RequestMapping("/accessToken")
public class AccessTokenApi {

    private final AccessTokenManage accessTokenManage;

    public AccessTokenApi(AccessTokenManage accessTokenManage) {
        this.accessTokenManage = accessTokenManage;
    }

    /**
     * 生成 AccessToken
     */
    @PostMapping("/generate")
    public AccessTokenVO generate(@RequestBody GenerateRequest request) {
        Long creator = AuthUtils.getUser().getUserId();
        Long expireTime = request.getExpireTime(); // null 表示永不过期
        
        AccessToken accessToken = accessTokenManage.generateAccessToken(
            request.getName(), 
            creator, 
            expireTime
        );
        
        LogUtil.info("User " + creator + " generated AccessToken: " + accessToken.getAccessToken());
        
        AccessTokenVO vo = new AccessTokenVO();
        vo.setId(accessToken.getId());
        vo.setAccessToken(accessToken.getAccessToken());
        vo.setAccessSecret(accessToken.getAccessSecret()); // 只在生成时返回一次
        vo.setName(accessToken.getName());
        vo.setCreator(accessToken.getCreator());
        vo.setCreateTime(accessToken.getCreateTime());
        vo.setExpireTime(accessToken.getExpireTime());
        vo.setEnable(accessToken.getEnable());
        
        return vo;
    }

    /**
     * 查询 AccessToken 列表
     */
    @GetMapping("/list")
    public List<AccessTokenVO> list() {
        List<AccessToken> tokens = accessTokenManage.listAll();
        return tokens.stream()
                .map(token -> {
                    AccessTokenVO vo = new AccessTokenVO();
                    vo.setId(token.getId());
                    vo.setAccessToken(token.getAccessToken());
                    vo.setAccessSecret(null); // 查询时不返回 secret
                    vo.setName(token.getName());
                    vo.setCreator(token.getCreator());
                    vo.setCreateTime(token.getCreateTime());
                    vo.setUpdateTime(token.getUpdateTime());
                    vo.setExpireTime(token.getExpireTime());
                    vo.setEnable(token.getEnable());
                    vo.setLastUseTime(token.getLastUseTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 禁用 AccessToken
     */
    @PostMapping("/disable/{id}")
    public void disable(@PathVariable Long id) {
        accessTokenManage.disableAccessToken(id);
        LogUtil.info("AccessToken disabled: " + id);
    }

    /**
     * 删除 AccessToken
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        accessTokenManage.deleteAccessToken(id);
        LogUtil.info("AccessToken deleted: " + id);
    }

    /**
     * 生成请求
     */
    public static class GenerateRequest {
        private String name;
        private Long expireTime; // 过期时间戳，null 表示永不过期

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(Long expireTime) {
            this.expireTime = expireTime;
        }
    }
}

