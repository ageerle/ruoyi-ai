package org.ruoyi.service.report.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.dto.request.AiReportExecuteRequest;
import org.ruoyi.domain.dto.request.AiReportGenerateRequest;
import org.ruoyi.domain.dto.request.AiReportRefineRequest;
import org.ruoyi.domain.dto.response.AiReportResponse;
import org.ruoyi.service.report.IAiReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportServiceImpl implements IAiReportService {

    private static final Pattern MYSQL_JDBC_PATTERN =
        Pattern.compile("^jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+).*$");

    private static final int DEFAULT_MAX_ROWS = 100;
    private static final int ABSOLUTE_MAX_ROWS = 1000;

    private final IChatModelService chatModelService;
    private final ObjectMapper objectMapper;

    @Value("${spring.datasource.dynamic.datasource.master.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.dynamic.datasource.master.username:}")
    private String dbUsername;

    @Value("${spring.datasource.dynamic.datasource.master.password:}")
    private String dbPassword;

    @Override
    public AiReportResponse generate(AiReportGenerateRequest request) {
        ChatModel model = buildModel(request.getModel());
        int maxRows = resolveMaxRows(request.getMaxRows());

        String tableInfo = executeShellSql("SHOW TABLES", 10);

        String sqlPlanPrompt = """
            你是数据分析师。基于用户需求和表信息，只生成一个安全的 SELECT SQL。
            返回 JSON，不要返回解释：
            {"title":"","summary":"","sql":""}

            约束：
            1) SQL 必须是 SELECT 开头
            2) 严禁写入/删除/更新语句
            3) 尽量不要 SELECT *
            4) 若用户未指定行数，默认 LIMIT %d

            表信息（来自 shell 查询）：
            %s

            用户需求：
            %s
            """.formatted(maxRows, tableInfo, request.getPrompt());

        String planRaw = model.chat(sqlPlanPrompt);
        JsonNode plan = parseJson(planRaw);

        String title = plan.path("title").asText("AI 报表");
        String summary = plan.path("summary").asText("根据自然语言需求自动生成");
        String sql = normalizeSql(plan.path("sql").asText(""));
        validateSelectSql(sql);

        return AiReportResponse.builder()
            .title(title)
            .summary(summary)
            .sql(sql)
            .queryResult("")
            .html("")
            .build();
    }

    @Override
    public AiReportResponse execute(AiReportExecuteRequest request) {
        ChatModel model = buildModel(request.getModel());
        String sql = normalizeSql(request.getSql());
        validateSelectSql(sql);

        String queryResult = executeShellSql(sql, 30);

        String htmlPrompt = """
            你是前端报表工程师。请生成一个完整的 HTML 页面（只输出 HTML，不要 markdown 代码块）。
            页面要求：
            1) 展示标题、摘要、SQL、查询结果（保留原始文本）
            2) 风格专业，适合企业报表
            3) 页面包含一个“继续编辑”输入框和按钮
            4) 点击按钮后调用 POST /chat/report/refine
               JSON: {"model":"%s","prompt":"用户输入","html":"当前页面 outerHTML","dataContext":"%s"}
            5) 收到返回后，用返回的 html 替换当前页面

            标题：%s
            摘要：%s
            SQL：%s
            查询结果：
            %s
            """.formatted(request.getModel(), escapeForJson(queryResult), request.getTitle(), request.getSummary(), sql, queryResult);

        String html = stripCodeFence(model.chat(htmlPrompt));

        return AiReportResponse.builder()
            .title(request.getTitle())
            .summary(request.getSummary())
            .sql(sql)
            .queryResult(queryResult)
            .html(html)
            .build();
    }

    @Override
    public AiReportResponse refine(AiReportRefineRequest request) {
        ChatModel model = buildModel(request.getModel());
        String dataContext = request.getDataContext() == null ? "" : request.getDataContext();

        String refinePrompt = """
            你是前端报表工程师。请基于当前 HTML 和用户要求进行修改。
            仅输出完整 HTML，不要解释。

            修改要求：
            %s

            数据上下文：
            %s

            当前 HTML：
            %s
            """.formatted(request.getPrompt(), dataContext, request.getHtml());

        String updatedHtml = stripCodeFence(model.chat(refinePrompt));

        return AiReportResponse.builder()
            .title("AI 报表（已编辑）")
            .summary(request.getPrompt())
            .sql("")
            .queryResult(dataContext)
            .html(updatedHtml)
            .build();
    }

    private String executeShellSql(String sql, int timeoutSeconds) {
        MysqlConnectionInfo info = parseMysqlConnection(jdbcUrl);
        List<String> command = new ArrayList<>();
        command.add("mysql");
        command.add("--batch");
        command.add("--raw");
        command.add("--default-character-set=utf8mb4");
        command.add("-h");
        command.add(info.host());
        command.add("-P");
        command.add(String.valueOf(info.port()));
        command.add("-u");
        command.add(dbUsername);
        command.add("-D");
        command.add(info.database());
        command.add("-e");
        command.add(sql);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);

        try {
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                output = sb.toString().trim();
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("shell 查询超时");
            }

            int code = process.exitValue();
            if (code != 0) {
                throw new IllegalArgumentException("shell 查询失败: " + output);
            }
            return output;
        } catch (Exception e) {
            log.error("shell 查询执行失败, sql={}", sql, e);
            throw new IllegalArgumentException("shell 查询失败: " + e.getMessage());
        }
    }

    private MysqlConnectionInfo parseMysqlConnection(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("未配置 MySQL JDBC URL");
        }

        Matcher matcher = MYSQL_JDBC_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无法解析 JDBC URL: " + url);
        }

        String host = matcher.group(1);
        int port = matcher.group(2) == null ? 3306 : Integer.parseInt(matcher.group(2));
        String database = matcher.group(3);
        if (database.contains("/")) {
            database = database.substring(0, database.indexOf('/'));
        }
        return new MysqlConnectionInfo(host, port, database);
    }

    private ChatModel buildModel(String modelName) {
        ChatModelVo modelVo = chatModelService.selectModelByName(modelName);
        if (modelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + modelName);
        }

        return OpenAiChatModel.builder()
            .baseUrl(modelVo.getApiHost())
            .apiKey(modelVo.getApiKey())
            .modelName(modelVo.getModelName())
            .build();
    }

    private JsonNode parseJson(String raw) {
        try {
            String candidate = raw.trim();
            if (candidate.startsWith("```") && candidate.contains("{")) {
                int start = candidate.indexOf('{');
                int end = candidate.lastIndexOf('}');
                candidate = candidate.substring(start, end + 1);
            }
            return objectMapper.readTree(candidate);
        } catch (Exception e) {
            log.error("解析 SQL 计划 JSON 失败: {}", raw, e);
            throw new IllegalArgumentException("模型返回格式错误，未能解析 SQL 计划");
        }
    }

    private String stripCodeFence(String content) {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```")) {
            int firstLineBreak = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLineBreak > -1 && lastFence > firstLineBreak) {
                return value.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return value;
    }

    private int resolveMaxRows(Integer maxRows) {
        if (maxRows == null || maxRows < 1) {
            return DEFAULT_MAX_ROWS;
        }
        return Math.min(maxRows, ABSOLUTE_MAX_ROWS);
    }

    private String normalizeSql(String sql) {
        String value = sql == null ? "" : sql.trim();
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private void validateSelectSql(String sql) {
        if (sql.isBlank()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String upperSql = sql.toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            throw new IllegalArgumentException("仅允许 SELECT SQL");
        }
        if (upperSql.contains(";") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ")
            || upperSql.contains("INSERT ") || upperSql.contains("DROP ") || upperSql.contains("ALTER ")) {
            throw new IllegalArgumentException("SQL 含有不允许的语句");
        }
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record MysqlConnectionInfo(String host, int port, String database) {
    }
}
