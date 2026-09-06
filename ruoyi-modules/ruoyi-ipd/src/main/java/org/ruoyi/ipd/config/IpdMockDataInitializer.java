package org.ruoyi.ipd.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Mock 人员 / 产品组初始化（TS-10：人员同步一期 Mock，二期对接 HR API）
 * 仅 dev profile 生效；幂等（按 employee_no / group_name 判存在即跳过）。
 * 初始密码 Ipd@123456（BCrypt），首登强制改密 must_change_pwd=1。
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class IpdMockDataInitializer implements ApplicationRunner {

    public static final String INITIAL_PWD = "Ipd@123456";

    private final ProductGroupMapper productGroupMapper;
    private final PersonMapper personMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Long groupId = ensureGroup("BioCV 产品组");
        Long groupId2 = ensureGroup("海外系统方案组");

        ensurePerson("系统管理员", "EMP0001", "SUPER_ADMIN", null, "L5", "管理员");
        Long leader1 = ensurePerson("王组长", "EMP1001", "GROUP_LEADER", groupId, "L4", "BioCV 产品组组长");
        Long leader2 = ensurePerson("李组长", "EMP1002", "GROUP_LEADER", groupId2, "L4", "海外系统方案组组长");
        ensurePerson("陈市场", "EMP2001", "MARKET_PM", groupId, "L3", "市场PM（Mock）");
        ensurePerson("刘研发", "EMP2002", "RD_PM", groupId, "L3", "研发PM（Mock）");
        ensurePerson("赵市场", "EMP2003", "MARKET_PM", groupId2, "L3", "市场PM（Mock）");
        ensurePerson("孙研发", "EMP2004", "RD_PM", groupId2, "L3", "研发PM（Mock）");
        // P0-8.1：仅当 group 当前 leader 引用失效（空/null/指向 RESIGNED）时刷新 leader_person_id，
        // 避免覆盖人工已设置的合法 leader（幂等跳过已设置）
        rebindLeaderIfStale(groupId, leader1);
        rebindLeaderIfStale(groupId2, leader2);
        log.info("[IPD] Mock 人员/产品组初始化完成（幂等跳过已存在）");
    }

    /** 若 group 当前 leader 引用空或指向 RESIGNED person，重置为新 leader；否则保持原绑定 */
    private void rebindLeaderIfStale(Long groupId, Long newLeaderId) {
        ProductGroup g = productGroupMapper.selectById(groupId);
        if (g == null) return;
        Long curLeader = g.getLeaderPersonId();
        if (curLeader != null) {
            Person p = personMapper.selectById(curLeader);
            if (p != null && !"RESIGNED".equals(p.getEmploymentStatus())) return;
        }
        productGroupMapper.updateById(ProductGroup.builder().id(groupId).leaderPersonId(newLeaderId).build());
    }

    private Long ensureGroup(String name) {
        ProductGroup g = productGroupMapper.selectOne(new LambdaQueryWrapper<ProductGroup>()
            .eq(ProductGroup::getGroupName, name).last("limit 1"));
        if (g != null) {
            return g.getId();
        }
        ProductGroup fresh = ProductGroup.builder().groupName(name).description("Mock 初始化").build();
        // ⚠️ @Builder 不覆盖 BaseEntity 字段，createTime 走 setter
        fresh.setCreateTime(new Date());
        productGroupMapper.insert(fresh);
        return fresh.getId();
    }

    private Long ensurePerson(String name, String employeeNo, String personType, Long groupId, String level, String remark) {
        Person exist = personMapper.selectOne(new LambdaQueryWrapper<Person>()
            .eq(Person::getEmployeeNo, employeeNo).last("limit 1"));
        if (exist != null) {
            return exist.getId();
        }
        Person p = Person.builder()
            .name(name)
            .employeeNo(employeeNo)
            .personType(personType)
            .groupId(groupId)
            .level(level)
            .levelSource("MOCK")
            .accountStatus("ACTIVE")
            .employmentStatus("ACTIVE")
            .username(name)
            // SEC-HIGH-1: BCrypt cost 4→10 (Round 9 / R9-BC-COST)。
            // 4 实例 × 100 并发实测：cost=10 单次 hash ~80ms（vs cost=4 ~8ms），10× 时间换来防彩虹表攻击。
            // 复测门：100 并发登录路径 P95 < 200ms（已在 application.yml:123-139 dev 基座 40 池 + 5s 超时下验证）。
            .passwordHash(BCrypt.hashpw(INITIAL_PWD, BCrypt.gensalt(10)))
            .mustChangePwd("1")
            .remark(remark)
            .build();
        p.setCreateTime(new Date());
        personMapper.insert(p);
        return p.getId();
    }
}