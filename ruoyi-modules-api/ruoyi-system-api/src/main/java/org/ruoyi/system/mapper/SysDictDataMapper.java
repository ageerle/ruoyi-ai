package org.ruoyi.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.core.constant.UserConstants;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.SysDictData;
import org.ruoyi.system.domain.vo.SysDictDataVo;

import java.util.List;

/**
 * 字典表 数据层
 *
 * @author Lion Li
 */
@Mapper
public interface SysDictDataMapper extends BaseMapperPlus<SysDictData, SysDictDataVo> {

    default List<SysDictDataVo> selectDictDataByType(String dictType) {
        return selectVoList(
                new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getStatus, UserConstants.DICT_NORMAL)
                        .eq(SysDictData::getDictType, dictType)
                        .orderByAsc(SysDictData::getDictSort));
    }
}
