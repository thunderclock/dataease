package io.dataease.license.utils;

import io.dataease.license.bo.F2CLicResult;

/**
 * 自定义许可证工具类 - 移除社区版本限制
 * 修改说明：此文件用于绕过DataEase社区版本的功能限制
 * 原始版权：DataEase开源项目
 * 修改时间：2024年
 */
public class LicenseUtil {
    
    /**
     * 始终返回true，表示许可证有效
     * 这样可以绕过所有企业版功能限制
     */
    public static boolean licenseValid() {
        return true;
    }

    /**
     * 验证许可证
     * 始终返回true，表示许可证有效
     */
    public static boolean validate() {
        return true;
    }
    
    /**
     * 检查是否为社区版本
     * 返回false表示不是社区版本，从而启用所有功能
     */
    public static boolean isCommunityVersion() {
        return false;
    }
    
    /**
     * 获取许可证信息
     * 返回一个模拟的企业版许可证信息
     */
    public static String getLicenseInfo() {
        return "Custom Enterprise License - Internal Use Only";
    }

    /**
     * 获取许可证信息
     * 返回一个模拟的企业版许可证信息
     */
    public static F2CLicResult get() {
        F2CLicResult f2CLicResult = new F2CLicResult();
        f2CLicResult.setStatus(F2CLicResult.Status.valid);
        return f2CLicResult;
    }
}
