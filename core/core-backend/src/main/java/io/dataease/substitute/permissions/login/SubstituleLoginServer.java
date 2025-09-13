package io.dataease.substitute.permissions.login;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.dataease.api.permissions.login.dto.PwdLoginDTO;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.config.SubstituleLoginConfig;
import io.dataease.auth.vo.TokenVO;
import io.dataease.exception.DEException;
import io.dataease.i18n.Translator;
import io.dataease.user.entity.User;
import io.dataease.user.manage.UserManage;
import io.dataease.utils.LogUtil;
import io.dataease.utils.Md5Utils;
import io.dataease.utils.RsaUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Component
@ConditionalOnMissingBean(name = "loginServer")
@RestController
@RequestMapping
public class SubstituleLoginServer {

    @Autowired
    private UserManage userManage;

    @PostMapping("/login/localLogin")
    public TokenVO localLogin(@RequestBody PwdLoginDTO dto, HttpServletRequest request) {
        String name = dto.getName();
        String pwd = dto.getPwd();
        
        LogUtil.info("Received login request - original name: " + name + ", original pwd: " + pwd);
        
        // 尝试RSA解密，如果失败则使用明文（开发模式）
        try {
            String decryptedName = RsaUtils.decryptStr(name);
            String decryptedPwd = RsaUtils.decryptStr(pwd);
            LogUtil.info("RSA decryption success, decrypted username: " + decryptedName + ", decrypted password: " + decryptedPwd);
            name = decryptedName;
            pwd = decryptedPwd;
        } catch (Exception e) {
            // RSA解密失败，可能是开发环境或客户端加密问题，尝试使用明文
            LogUtil.warn("RSA decryption failed, trying plain text: " + e.getMessage());
            LogUtil.info("Using original values as plain text - name: " + name + ", pwd: " + pwd);
            // 不进行解密，直接使用原始值
        }

        dto.setName(name);
        dto.setPwd(pwd);

        LogUtil.info("Final login attempt - username: " + name + ", password: " + pwd);

        TokenUserBO tokenUserBO;
        String md5Pwd;

        // 如果账号是admin，则验证admin密码
        if (StringUtils.equals("admin", name)) {
            if (!StringUtils.equals(pwd, SubstituleLoginConfig.getPwd())) {
                DEException.throwException(Translator.get("i18n_login_name_pwd_err"));
            }
            tokenUserBO = new TokenUserBO();
            tokenUserBO.setUserId(1L);
            tokenUserBO.setDefaultOid(1L);
            md5Pwd = Md5Utils.md5(pwd);
        } else {
            // 使用用户服务验证普通用户
            User user = userManage.authenticateUser(name, pwd);
            if (user == null) {
                DEException.throwException(Translator.get("i18n_login_name_pwd_err"));
            }
            
            // 检查用户是否启用
            if (!user.getEnable()) {
                DEException.throwException("用户已被禁用");
            }
            
            tokenUserBO = new TokenUserBO();
            tokenUserBO.setUserId(user.getId());
            tokenUserBO.setDefaultOid(user.getOrgId() != null ? user.getOrgId() : 1L);
            md5Pwd = Md5Utils.md5(pwd);
            
            // 更新最后登录信息
            userManage.updateLastLogin(user.getId(), getClientIp(request));
        }
        
        return generate(tokenUserBO, md5Pwd);
    }


    @GetMapping("/logout")
    public void logout() {
        LogUtil.info("substitule logout");
    }

    private TokenVO generate(TokenUserBO bo, String secret) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Long userId = bo.getUserId();
        Long defaultOid = bo.getDefaultOid();
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("uid", userId).withClaim("oid", defaultOid);
        String token = builder.sign(algorithm);
        return new TokenVO(token, 0L);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }
}
