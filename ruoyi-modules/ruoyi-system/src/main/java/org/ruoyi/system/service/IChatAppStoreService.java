package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.ChatAppStoreVo;
import org.ruoyi.system.domain.bo.ChatAppStoreBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.system.request.RoleListDto;
import org.ruoyi.system.request.RoleRequest;
import org.ruoyi.system.request.SimpleGenerateRequest;
import org.ruoyi.system.response.SimpleGenerateDataResponse;
import org.ruoyi.system.response.rolelist.ChatAppStoreVO;

import java.util.Collection;
import java.util.List;

/**
 * 应用市场Service接口
 *
 * @author Lion Li
 * @date 2024-03-19
 */
public interface IChatAppStoreService {

    /**
     * 查询应用市场
     */
    ChatAppStoreVo queryById(Long id);

    /**
     * 查询应用市场列表
     */
    TableDataInfo<ChatAppStoreVo> queryPageList(ChatAppStoreBo bo, PageQuery pageQuery);

    /**
     * 查询应用市场列表
     */
    List<ChatAppStoreVo> queryList(ChatAppStoreBo bo);

    /**
     * 新增应用市场
     */
    Boolean insertByBo(RoleRequest roleRequest);

    /**
     * 生成音频
     */
    SimpleGenerateDataResponse simpleGenerate(SimpleGenerateRequest simpleGenerateRequest);

    /**
     * 修改应用市场
     */
    Boolean updateByBo(ChatAppStoreBo bo);

    /**
     * 校验并批量删除应用市场信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询市场角色
     *
     * @return 角色列表
     */
    List<ChatAppStoreVO> roleList();

    /**
     * 收藏市场角色
     *
     */
    void copyRole(RoleListDto roleListDto);
}
