package org.ruoyi.service;


import org.ruoyi.domain.bo.ChatAgentManageBo;
import org.ruoyi.domain.vo.ChatAgentManageVo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 智能体管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatAgentManageService {

    /**
     * 查询智能体管理
     */
    ChatAgentManageVo queryById(Long id);

    /**
     * 查询智能体管理列表
     */
    TableDataInfo<ChatAgentManageVo> queryPageList(ChatAgentManageBo bo, PageQuery pageQuery);

    /**
     * 查询智能体管理列表
     */
    List<ChatAgentManageVo> queryList(ChatAgentManageBo bo);

    /**
     * 新增智能体管理
     */
    Boolean insertByBo(ChatAgentManageBo bo);

    /**
     * 修改智能体管理
     */
    Boolean updateByBo(ChatAgentManageBo bo);

    /**
     * 校验并批量删除智能体管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
