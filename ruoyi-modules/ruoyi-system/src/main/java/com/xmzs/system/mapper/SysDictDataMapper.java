package com.xmzs.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xmzs.common.core.constant.UserConstants;
import com.xmzs.system.domain.SysDictData;
import com.xmzs.common.mybatis.core.mapper.BaseMapperPlus;
import com.xmzs.system.domain.vo.SysDictDataVo;

import java.util.List;

/**
 * 字典表 数据层
 *
 * @author Lion Li
 */
public interface SysDictDataMapper extends BaseMapperPlus<SysDictData, SysDictDataVo> {

    default List<SysDictDataVo> selectDictDataByType(String dictType) {
        return selectVoList(
            new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getStatus, UserConstants.DICT_NORMAL)
                .eq(SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictSort));
    }
}
