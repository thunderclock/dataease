package io.dataease.role.server;

import io.dataease.api.permissions.role.api.RoleApi;
import io.dataease.api.permissions.role.dto.*;
import io.dataease.api.permissions.role.vo.*;
import io.dataease.model.KeywordRequest;
import io.dataease.role.manage.RoleManage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色服务端实现类
 */
@RestController
@RequestMapping("/role")
public class RoleServer implements RoleApi {

    @Autowired
    private RoleManage roleManage;

    @Override
    @PostMapping("/query")
    public List<RoleVO> query(@RequestBody KeywordRequest request) {
        return roleManage.query(request);
    }

    @Override
    @GetMapping("/detail/{rid}")
    public RoleDetailVO detail(@PathVariable("rid") Long rid) {
        return roleManage.detail(rid);
    }

    @Override
    @PostMapping("/create")
    public Long create(@RequestBody RoleCreator creator) {
        return roleManage.create(creator);
    }

    @Override
    @PostMapping("/edit")
    public void edit(@RequestBody RoleEditor editor) {
        roleManage.edit(editor);
    }

    @Override
    @PostMapping("/delete/{rid}")
    public void delete(@PathVariable("rid") Long rid) {
        roleManage.delete(rid);
    }

    @Override
    @PostMapping("/user/option")
    public List<RoleVO> optionForUser(@RequestBody RoleRequest request) {
        return roleManage.optionForUser(request);
    }

    @Override
    @PostMapping("/user/selected")
    public List<RoleVO> selectedForUser(@RequestBody RoleRequest request) {
        return roleManage.selectedForUser(request);
    }

    @Override
    @PostMapping("/unMountUser")
    public void unMountUser(@RequestBody UnmountUserRequest request) {
        roleManage.unMountUser(request);
    }

    @Override
    @PostMapping("/mountUser")
    public void mountUser(@RequestBody MountUserRequest request) {
        roleManage.mountUser(request);
    }

    @Override
    @PostMapping("/beforeUnmountInfo")
    public Integer beforeUnmountInfo(@RequestBody UnmountUserRequest request) {
        return roleManage.beforeUnmountInfo(request);
    }

    @Override
    @GetMapping("/searchExternalUser/{keyword}")
    public ExternalUserVO searchExternalUser(@PathVariable("keyword") String keyword) {
        return roleManage.searchExternalUser(keyword);
    }

    @Override
    @PostMapping("/mountExternalUser")
    public void mountExternalUser(@RequestBody MountExternalUserRequest request) {
        roleManage.mountExternalUser(request);
    }

    @Override
    @PostMapping("/copy")
    public void copy(@RequestBody RoleCopyRequest request) {
        roleManage.copy(request);
    }

    @Override
    @PostMapping("/byCurOrg")
    public List<RoleVO> byCurOrg(@RequestBody KeywordRequest request) {
        return roleManage.byCurOrg(request);
    }

    @Override
    @GetMapping("/queryWithOid/{oid}")
    public List<RoleVO> queryWithOid(@PathVariable("oid") Long oid) {
        return roleManage.queryWithOid(oid);
    }
}
