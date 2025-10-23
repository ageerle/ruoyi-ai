package org.ruoyi.mapper;


import org.apache.ibatis.annotations.*;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.McpInfo;
import org.ruoyi.domain.vo.McpInfoVo;

import java.util.List;

/**
 * MCPMapper接口
 *
 * @author jiuyi
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@Mapper
public interface McpInfoMapper extends BaseMapperPlus<McpInfo, McpInfoVo> {
    @Select("SELECT * FROM mcp_info WHERE server_name = #{serverName}")
    McpInfo selectByServerName(@Param("serverName") String serverName);

    @Select("SELECT * FROM mcp_info WHERE status = 1")
    List<McpInfo> selectActiveServers();

    @Select("SELECT server_name FROM mcp_info WHERE status = 1")
    List<String> selectActiveServerNames();

    @Update("UPDATE mcp_info SET status = #{status} WHERE server_name = #{serverName}")
    int updateActiveStatus(@Param("serverName") String serverName, @Param("status") Boolean status);

    @Delete("DELETE FROM mcp_info WHERE server_name = #{serverName}")
    int deleteByServerName(@Param("serverName") String serverName);
}
