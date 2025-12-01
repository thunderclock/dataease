package io.dataease.auth.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * AccessKey VO
 */
@Data
public class AccessKeyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String accessKey;
    private String accessSecret; // 只在生成时返回一次
    private String name;
    private Long creator;
    private Long userId; // 绑定的用户ID
    private String userName; // 绑定的用户名（用于显示）
    private Long createTime;
    private Long updateTime;
    private Long expireTime;
    private Boolean enable;
    private Long lastUseTime;
}

