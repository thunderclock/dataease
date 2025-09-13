package io.dataease.user.server;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.dataease.api.permissions.login.dto.MfaLoginDTO;
import io.dataease.api.permissions.login.vo.MfaQrVO;
import io.dataease.api.permissions.role.dto.UserRequest;
import io.dataease.api.permissions.user.api.UserApi;
import io.dataease.api.permissions.user.dto.*;
import io.dataease.api.permissions.user.vo.*;
import io.dataease.auth.vo.TokenVO;
import io.dataease.model.KeywordRequest;
import io.dataease.user.manage.UserManage;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户服务端实现类
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/user")
public class UserServer implements UserApi {

    @Autowired
    private UserManage userManage;

    @Override
    @Operation(summary = "查询用户列表")
    @Parameters({
            @Parameter(name = "goPage", description = "目标页码", required = true, in = ParameterIn.PATH),
            @Parameter(name = "pageSize", description = "每页容量", required = true, in = ParameterIn.PATH),
            @Parameter(name = "request", description = "过滤条件", required = true)
    })
    @PostMapping("/pager/{goPage}/{pageSize}")
    public IPage<UserGridVO> pager(@PathVariable("goPage") int goPage, 
                                   @PathVariable("pageSize") int pageSize, 
                                   @RequestBody UserGridRequest request) {
        return userManage.pager(goPage, pageSize, request);
    }

    @Override
    @Operation(summary = "查询用户详情")
    @Parameter(name = "id", description = "ID", required = true, in = ParameterIn.PATH)
    @GetMapping("/queryById/{id}")
    public UserFormVO queryById(@PathVariable("id") Long id) {
        return userManage.queryById(id);
    }

    @Override
    @Operation(summary = "查询个人信息")
    @GetMapping("/personInfo")
    public UserFormVO personInfo() {
        return userManage.personInfo();
    }

    @Override
    @Operation(summary = "查询用户系统变量信息")
    @GetMapping("/personSysVariableInfo/{id}")
    public UserGridVO personSysVariableInfo(@PathVariable("id") Long id) {
        // 简化实现
        UserFormVO userFormVO = userManage.queryById(id);
        UserGridVO userGridVO = new UserGridVO();
        org.springframework.beans.BeanUtils.copyProperties(userFormVO, userGridVO);
        return userGridVO;
    }

    @Override
    @Operation(summary = "查询客户端IP信息")
    @GetMapping("/ipInfo")
    public CurIpVO ipInfo() {
        CurIpVO curIpVO = new CurIpVO();
        curIpVO.setIp("127.0.0.1");
        return curIpVO;
    }

    @Override
    @Operation(summary = "创建")
    @PostMapping("/create")
    public void create(@RequestBody UserCreator creator) {
        userManage.create(creator);
    }

    @Override
    @Operation(summary = "创建第三方用户")
    @PostMapping("/createPlatform")
    public void createPlatform(@RequestBody PlatformUserCreator creator) {
        // 简化实现，转换为普通用户创建
        UserCreator userCreator = new UserCreator();
        org.springframework.beans.BeanUtils.copyProperties(creator, userCreator);
        userManage.create(userCreator);
    }

    @Override
    @Operation(summary = "编辑")
    @PostMapping("/edit")
    public void edit(@RequestBody UserEditor editor) {
        userManage.edit(editor);
    }

    @Override
    @Operation(summary = "变更个人信息")
    @PostMapping("/personEdit")
    public void personEdit(@RequestBody UserEditor editor) {
        userManage.edit(editor);
    }

    @Override
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "ID", required = true, in = ParameterIn.PATH)
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable("id") Long id) {
        userManage.delete(id);
    }

    @Override
    @Operation(summary = "批量删除")
    @PostMapping("/batchDel")
    public void batchDel(@RequestBody List<Long> ids) {
        userManage.batchDel(ids);
    }

    @Override
    @Operation(summary = "角色可绑用户")
    @PostMapping("/role/option")
    public List<UserItemVO> optionForRole(@RequestBody UserRequest request) {
        // 简化实现
        return List.of();
    }

    @Override
    @Operation(summary = "组织内用户")
    @GetMapping("/org/option")
    public List<UserItemVO> optionForOrg() {
        // 简化实现
        return List.of();
    }

    @Override
    @Operation(summary = "角色已绑用户")
    @Parameters({
            @Parameter(name = "goPage", description = "目标页码", required = true, in = ParameterIn.PATH),
            @Parameter(name = "pageSize", description = "每页容量", required = true, in = ParameterIn.PATH),
            @Parameter(name = "request", description = "过滤条件", required = true)
    })
    @PostMapping("/role/selected/{goPage}/{pageSize}")
    public IPage<UserItemVO> selectedForRole(@PathVariable("goPage") int goPage, 
                                             @PathVariable("pageSize") int pageSize, 
                                             @RequestBody UserRequest request) {
        // 简化实现
        return null;
    }

    @Override
    @Operation(summary = "切换组织")
    @Parameter(name = "oId", description = "目标组织ID", required = true, in = ParameterIn.PATH)
    @PostMapping("/switch/{oId}")
    public TokenVO switchOrg(@PathVariable("oId") Long oId) {
        // 简化实现
        TokenVO tokenVO = new TokenVO();
        tokenVO.setToken("mock-token");
        return tokenVO;
    }

    @Override
    @Operation(summary = "获取当前登录人信息")
    @GetMapping("/info")
    public CurUserVO info() {
        return userManage.info();
    }

    @Override
    @Operation(summary = "查询当前组织内用户")
    @PostMapping("/byCurOrg")
    public List<UserItem> byCurOrg(@RequestBody KeywordRequest request) {
        return userManage.byCurOrg(request);
    }

    @Override
    @Operation(summary = "用户数量", hidden = true)
    @Hidden
    @GetMapping("/userCount")
    public int userCount() {
        return userManage.userCount();
    }

    @Override
    @Operation(summary = "切换语言")
    @PostMapping("/switchLanguage")
    public void switchLanguage(@RequestBody LangSwitchRequest request) {
        // 简化实现
    }

    @Override
    @Operation(summary = "下载批量导入模版")
    @PostMapping("/excelTemplate")
    public void excelTemplate() {
        // 简化实现
    }

    @Override
    @Operation(summary = "批量导入")
    @PostMapping("/batchImport")
    public UserImportVO batchImport(@RequestPart(value = "file") MultipartFile file) {
        // 简化实现
        UserImportVO userImportVO = new UserImportVO();
        userImportVO.setSuccessCount(0);
        userImportVO.setErrorCount(0);
        return userImportVO;
    }

    @Override
    @Operation(summary = "下载批量导入失败记录")
    @Parameter(name = "key", description = "导入结果key", required = true, in = ParameterIn.PATH)
    @GetMapping("/errorRecord/{key}")
    public void errorRecord(@PathVariable("key") String key) {
        // 简化实现
    }

    @Override
    @Operation(summary = "清理批量导入失败记录")
    @Parameter(name = "key", description = "导入结果key", required = true, in = ParameterIn.PATH)
    @GetMapping("/clearErrorRecord/{key}")
    public void clearErrorRecord(@PathVariable("key") String key) {
        // 简化实现
    }

    @Override
    @Operation(summary = "查询默认密码")
    @GetMapping("/defaultPwd")
    public String defaultPwd() {
        return userManage.getDefaultPassword();
    }

    @Override
    @Operation(summary = "重置为默认密码")
    @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH)
    @PostMapping("/resetPwd/{id}")
    public void resetPwd(@PathVariable("id") Long id) {
        userManage.resetPwd(id);
    }

    @Override
    @Operation(summary = "切换用户状态")
    @PostMapping("/enable")
    public void enable(@RequestBody EnableSwitchRequest request) {
        userManage.enable(request);
    }

    @Override
    @Operation(summary = "修改个人密码")
    @PostMapping("/modifyPwd")
    public void modifyPwd(@RequestBody ModifyPwdRequest request) {
        userManage.modifyPwd(request);
    }

    @Override
    @Hidden
    @GetMapping("/firstEchelon/{limit}")
    public List<Long> firstEchelon(@PathVariable("limit") Long limit) {
        // 简化实现
        return List.of();
    }

    @Override
    @Operation(summary = "根据账号查询用户")
    @GetMapping("/queryByAccount/{account}")
    public CurUserVO queryByAccount(@PathVariable("account") String account) {
        return userManage.queryByAccount(account);
    }

    @Override
    @Hidden
    @PostMapping("/all")
    public List<UserItem> allUser(@RequestBody KeywordRequest request) {
        return userManage.byCurOrg(request);
    }

    @Override
    @Hidden
    @PostMapping("/admin/bind")
    public void adminBind(@RequestBody AdminBindRequest request) {
        // 简化实现
    }

    @Override
    @Hidden
    @PostMapping("/bind")
    public void bind(@RequestBody UserBindRequest request) {
        // 简化实现
    }

    @Override
    @Operation(summary = "解除绑定")
    @PostMapping("/unBind/{origin}")
    public void unBind(@PathVariable("origin") Integer origin) {
        // 简化实现
    }

    @Override
    @Operation(summary = "绑定状态")
    @GetMapping("/bindStatus")
    public List<Integer> bindStatus() {
        // 简化实现
        return List.of();
    }

    @Override
    @Hidden
    @GetMapping("/getRecipient")
    public List<Map<String, Object>> getRecipient(@RequestBody UserReciRequest request) {
        // 简化实现
        return List.of();
    }

    @Override
    @Hidden
    @GetMapping("/orgAdmin")
    public boolean orgAdmin() {
        // 简化实现
        return false;
    }

    @Override
    @Hidden
    @GetMapping("/defaultOrgAdmin")
    public boolean defaultOrgAdmin() {
        // 简化实现
        return false;
    }

    @Override
    @Hidden
    @PostMapping("/subOrgUser")
    public List<UserItem> subOrgUser(@RequestBody List<Long> oidList) {
        // 简化实现
        return List.of();
    }

    @Override
    public List<Long> getRecipientUserIds(UserReciRequest request) {
        // 简化实现
        return List.of();
    }

    @Override
    public List<Long> getUserIdByAccount(String account) {
        // 简化实现
        return List.of();
    }

    @Override
    public List<Long> getUserIdByName(String name) {
        // 简化实现
        return List.of();
    }

    @Override
    public List<Map<String, Object>> listUserInfosByIds(List<Long> ids) {
        // 简化实现
        return List.of();
    }

    @Override
    @Operation(summary = "MFA二维码信息")
    @GetMapping("/mfaQr")
    public MfaQrVO mfaQr() {
        // 简化实现
        MfaQrVO mfaQrVO = new MfaQrVO();
        mfaQrVO.setImg("mock-qr-code");
        return mfaQrVO;
    }

    @Override
    @Operation(summary = "MFA绑定状态")
    @GetMapping("/mfabound")
    public Boolean mfaBound() {
        // 简化实现
        return false;
    }

    @Override
    @Operation(summary = "绑定MFA")
    @PostMapping("/mfaBind")
    public void mfaBind(@RequestBody MfaLoginDTO dto) {
        // 简化实现
    }

    @Override
    @Operation(summary = "解绑MFA")
    @PostMapping("/mfaUnbind/{code}")
    public String mfaUnbind(@PathVariable("code") String code) {
        // 简化实现
        return "success";
    }

    @Override
    @Operation(summary = "重置MFA绑定状态")
    @PostMapping("/mfaRest/{id}")
    public void resetBind(@PathVariable("id") Long id) {
        // 简化实现
    }

    @Override
    @Hidden
    @GetMapping("/lang")
    public String userLang() {
        // 简化实现
        return "zh-CN";
    }
}
