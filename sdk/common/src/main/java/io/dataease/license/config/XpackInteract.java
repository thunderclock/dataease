package io.dataease.license.config;

import java.lang.annotation.*;

/**
 * XpackInteract 注解
 * 用于标记需要与 Xpack 交互的组件或方法
 * 
 * 修改说明：此文件用于解决 XpackInteract 注解的依赖问题
 * 原始版权：DataEase开源项目
 * 修改时间：2024年
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XpackInteract {
    
    /**
     * 交互组件的名称
     * @return 组件名称
     */
    String value() default "";
    
    /**
     * 是否替换原有实现
     * @return 是否替换
     */
    boolean replace() default false;
    
    /**
     * 是否无效（禁用）
     * @return 是否无效
     */
    boolean invalid() default false;

    /**
     * 是否在之前执行
     * @return
     */
    boolean before() default false;

    /**
     * 是否递归
     * @return
     */
    boolean recursion() default false;

    /**
     * 是否原始
     * @return
     */
    boolean original() default false;
}
