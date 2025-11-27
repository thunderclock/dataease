package io.dataease.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AccessToken VO
 */
@Data
public class AccessTokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String accessToken;
    private String accessSecret; // 只在生成时返回一次
    private String name;
    private Long creator;
    private Long createTime;
    private Long updateTime;
    private Long expireTime;
    private Boolean enable;
    private Long lastUseTime;
}

