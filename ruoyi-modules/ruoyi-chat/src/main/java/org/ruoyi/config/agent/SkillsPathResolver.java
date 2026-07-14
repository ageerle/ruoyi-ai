package org.ruoyi.config.agent;

import java.nio.file.Path;

/**
 * 磁盘 Skills 目录路径解析器
 * <p>
 * langchain4j 的 ShellSkills 通过 FileSystemSkillLoader 从磁盘加载 SKILL.md，
 * 路径硬编码在 ChatServiceFacade 中。抽到此工具类供智能体管理端与聊天流程共用，
 * 避免两处路径漂移。
 *
 * @author ruoyi team
 */
public final class SkillsPathResolver {

    private SkillsPathResolver() {
    }

    /**
     * skills 目录相对项目根目录的路径
     */
    private static final String SKILLS_RELATIVE_PATH = "ruoyi-admin/src/main/resources/skills";

    /**
     * 返回磁盘 skills 目录的绝对路径
     */
    public static Path resolveSkillsPath() {
        String userDir = System.getProperty("user.dir");
        return Path.of(userDir, SKILLS_RELATIVE_PATH);
    }

}
