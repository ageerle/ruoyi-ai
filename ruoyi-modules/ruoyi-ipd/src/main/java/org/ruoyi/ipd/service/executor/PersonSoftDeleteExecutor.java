package org.ruoyi.ipd.service.executor;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * Person 软删除执行器（entityType=persons，P0-6.2）。
 * Person 使用 MyBatis-Plus @TableLogic（del_flag 列），
 * deleteById 会被改写为 UPDATE，但为了与 Project/Product 行为一致且不依赖全局拦截器，
 * 显式执行 UPDATE del_flag='1' 保证审计触发点和执行点对齐。
 * 幂等：已软删不重复写。
 */
@Component
@RequiredArgsConstructor
public class PersonSoftDeleteExecutor implements SoftDeleteExecutor<Person> {

    public static final String ENTITY_TYPE = "persons";

    private final PersonMapper personMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<Person> entityClass() {
        return Person.class;
    }

    @Override
    public void softDelete(Long id) {
        Person person = personMapper.selectById(id);
        if (person == null || "1".equals(person.getDelFlag())) {
            return;
        }
        int rows = personMapper.update(null, new LambdaUpdateWrapper<Person>()
            .eq(Person::getId, id)
            .eq(Person::getDelFlag, "0")
            .set(Person::getDelFlag, "1"));
        if (rows != 1) {
            throw new ServiceException("人员软删除未更新唯一记录: id=" + id);
        }
    }

    /**
     * 判断人员是否已软删或不存在。
     *
     * @param id 人员主键
     * @return true 表示应记 DELETE_NOOP
     */
    @Override
    public boolean isDeleted(Long id) {
        Person person = personMapper.selectById(id);
        return person == null || "1".equals(person.getDelFlag());
    }
}