package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.VoiceRoleVo;
import org.ruoyi.system.domain.bo.VoiceRoleBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.system.request.RoleListDto;
import org.ruoyi.system.request.RoleRequest;
import org.ruoyi.system.request.SimpleGenerateRequest;
import org.ruoyi.system.response.SimpleGenerateDataResponse;
import org.ruoyi.system.response.rolelist.RoleListVO;

import java.util.Collection;
import java.util.List;

/**
 * 配音角色Service接口
 *
 * @author Lion Li
 * @date 2024-03-19
 */
public interface IVoiceRoleService {

    /**
     * 查询配音角色
     */
    VoiceRoleVo queryById(Long id);

    /**
     * 查询配音角色列表
     */
    TableDataInfo<VoiceRoleVo> queryPageList(VoiceRoleBo bo, PageQuery pageQuery);

    /**
     * 查询配音角色列表
     */
    List<VoiceRoleVo> queryList(VoiceRoleBo bo);

    /**
     * 新增配音角色
     */
    Boolean insertByBo(RoleRequest roleRequest);

    /**
     * 生成音频
     */
    SimpleGenerateDataResponse simpleGenerate(SimpleGenerateRequest simpleGenerateRequest);

    /**
     * 修改配音角色
     */
    Boolean updateByBo(VoiceRoleBo bo);

    /**
     * 校验并批量删除配音角色信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询市场角色
     *
     * @return 角色列表
     */
    List<RoleListVO> roleList();

    /**
     * 收藏市场角色
     *
     */
    void copyRole(RoleListDto roleListDto);
}
