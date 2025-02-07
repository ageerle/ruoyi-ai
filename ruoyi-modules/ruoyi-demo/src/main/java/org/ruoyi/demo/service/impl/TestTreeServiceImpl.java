package org.ruoyi.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.demo.domain.TestTree;
import org.ruoyi.demo.domain.bo.TestTreeBo;
import org.ruoyi.demo.domain.vo.TestTreeVo;
import org.ruoyi.demo.mapper.TestTreeMapper;
import org.ruoyi.demo.service.ITestTreeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试树表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-07-26
 */
// @DS("slave") // 切换从库查询
@RequiredArgsConstructor
@Service
public class TestTreeServiceImpl implements ITestTreeService {

    private final TestTreeMapper baseMapper;

    @Override
    public TestTreeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    // @DS("slave") // 切换从库查询
    @Override
    public List<TestTreeVo> queryList(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<TestTree> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getTreeName()), TestTree::getTreeName, bo.getTreeName());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            TestTree::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestTreeBo bo) {
        TestTree add = MapstructUtils.convert(bo, TestTree.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestTreeBo bo) {
        TestTree update = MapstructUtils.convert(bo, TestTree.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(TestTree entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
