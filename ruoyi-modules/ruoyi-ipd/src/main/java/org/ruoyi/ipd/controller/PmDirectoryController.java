package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PM 人员目录（P0 域#2；对齐 ZK-IPD 原型 GET /api/pm-directory）。
 * 顶栏/招募/项目空间/协作圈选人下拉的统一数据源（含组名、序列、等级）。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PmDirectoryController {

    private final PersonMapper personMapper;
    private final ProductGroupMapper productGroupMapper;

    /** 在职（ACTIVE）人员目录：id/姓名/工号/角色/等级/所属组。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/pm-directory")
    public ApiV1Response<Map<String, Object>> directory() {
        List<Person> people = personMapper.selectList(new LambdaQueryWrapper<Person>()
            .eq(Person::getAccountStatus, "ACTIVE")
            .orderByAsc(Person::getId));
        Map<Long, String> groupNames = productGroupMapper.selectList(null).stream()
            .filter(g -> g.getGroupName() != null)
            .collect(Collectors.toMap(ProductGroup::getId, ProductGroup::getGroupName, (a, b) -> a));

        List<Map<String, Object>> directory = new ArrayList<>();
        for (Person p : people) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("employeeNo", p.getEmployeeNo());
            m.put("personType", p.getPersonType());
            m.put("level", p.getLevel());
            m.put("groupId", p.getGroupId());
            m.put("groupName", p.getGroupId() != null ? groupNames.get(p.getGroupId()) : null);
            directory.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("directory", directory);
        result.put("total", directory.size());
        return ApiV1Response.ok(result);
    }
}
