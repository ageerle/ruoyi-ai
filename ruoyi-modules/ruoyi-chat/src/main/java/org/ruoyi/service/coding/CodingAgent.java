package org.ruoyi.service.coding;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 编程智能体 AiServices 接口。
 *
 * <p>配合 {@code AiServices.builder(CodingAgent.class).chatModel(...).tools(...)} 构建。
 * 同步 {@code String chat(...)} 方案（方案 B）：工具执行过程中的 add/edit/delete/cmd 事件
 * 由工具内部通过 {@link CodingEventChannel} 实时推送，最终回复文本在 chat() 返回后一次性推 text。
 *
 * <p>{@code @SystemMessage} 约束 LLM 只能操作 workspace 内文件，并明确每个工具的用途，
 * 提升工具调用命中率。
 *
 * @author ageerle
 */
public interface CodingAgent {

    @SystemMessage("""
        你是一个编程助手，直接操作用户工作目录内的文件与命令。规则：
        1. 所有文件操作必须在 workspace 目录内，使用绝对路径；不要越界访问外部目录。
        2. 读取文件用 read_file，新建/覆盖文件用 write_file，修改已存在文件用 edit_file，
           删除文件或目录用 delete_file，查看目录结构用 list_directory，执行构建/运行命令用 execute_command。
        3. 修改文件前，先用 read_file 读取当前内容，避免覆盖丢失代码。
        4. 执行命令前说明意图，命令失败时读取输出排查。
        5. 完成任务后用一两句话总结做了什么。""")
    String chat(@UserMessage String userMessage);
}
