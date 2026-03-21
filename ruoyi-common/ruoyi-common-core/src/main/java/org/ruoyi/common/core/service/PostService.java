package org.ruoyi.common.core.service;

import java.util.List;
import java.util.Map;

/**
 * 通用 岗位服务
 *
 * @author AprilWind
 */
public interface PostService {

    /**
     * 根据岗位 ID 列表查询岗位名称映射关系
     *
     * @param postIds 岗位 ID 列表
     * @return Map，其中 key 为岗位 ID，value 为对应的岗位名称
     */
    Map<Long, String> selectPostNamesByIds(List<Long> postIds);

}
