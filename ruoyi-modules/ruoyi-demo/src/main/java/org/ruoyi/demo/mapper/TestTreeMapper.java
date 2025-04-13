package org.ruoyi.demo.mapper;

import org.ruoyi.annotation.DataColumn;
import org.ruoyi.annotation.DataPermission;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.demo.domain.TestTree;
import org.ruoyi.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
