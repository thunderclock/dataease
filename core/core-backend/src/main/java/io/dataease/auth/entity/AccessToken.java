package io.dataease.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AccessToken 实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("core_access_token")
public class AccessToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * AccessToken
     */
    @TableField("access_token")
    private String accessToken;

    /**
     * AccessSecret
     */
    @TableField("access_secret")
    private String accessSecret;

    /**
     * 名称/描述
     */
    @TableField("name")
    private String name;

    /**
     * 创建人ID
     */
    @TableField("creator")
    private Long creator;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Long createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Long updateTime;

    /**
     * 过期时间（时间戳，null 表示永不过期）
     */
    @TableField("expire_time")
    private Long expireTime;

    /**
     * 是否启用 1-启用 0-禁用
     */
    @TableField("enable")
    private Boolean enable;

    /**
     * 最后使用时间
     */
    @TableField("last_use_time")
    private Long lastUseTime;
}

