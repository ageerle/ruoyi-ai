package org.ruoyi.service.agent;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AgentBo;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.agent.SkillOptionVo;

import java.util.Collection;
import java.util.List;

/**
 * 智能体服务接口
 *
 * @author ruoyi team
 */
public interface IAgentService {

    /**
     * 分页查询智能体列表
     */
    TableDataInfo<AgentVo> queryPageList(AgentBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的智能体列表（用于导出）
     */
    List<AgentVo> queryList(AgentBo bo);

    /**
     * 根据ID查询智能体（展开 JSON 数组字段为 List，关联填充模型/工具/知识库名称）
     */
    AgentVo queryById(Long id);

    /**
     * 新增智能体
     */
    Boolean insertByBo(AgentBo bo);

    /**
     * 修改智能体
     */
    Boolean updateByBo(AgentBo bo);

    /**
     * 批量删除智能体
     */
    Boolean deleteByIds(Collection<Long> ids);

    /**
     * 查询启用的智能体下拉选项（用户端聊天页选择用，status=0）
     */
    List<AgentVo> queryEnabledOptions();

    /**
     * 列出磁盘上可用的 Skills（供管理端表单勾选用）
     */
    List<SkillOptionVo> listSkillOptions();

}
