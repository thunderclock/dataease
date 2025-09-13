package io.dataease.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("core_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 账号
     */
    @TableField("account")
    private String account;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 是否启用 1-启用 0-禁用
     */
    @TableField("enable")
    private Boolean enable;

    /**
     * 组织ID
     */
    @TableField("org_id")
    private Long orgId;

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
     * 创建人
     */
    @TableField("creator")
    private Long creator;

    /**
     * 语言设置
     */
    @TableField("language")
    private String language;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private Long lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 是否删除 1-已删除 0-未删除
     */
    @TableField("deleted")
    private Boolean deleted;
}
