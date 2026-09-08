package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


public interface ChartGenerationAgent {

    @SystemMessage("""
            You are a chart generation specialist. Your only task is to generate Apache ECharts
            chart configurations. On success respond with ONLY the ECharts configuration in ```echarts
            markdown code block format, without surrounding explanations.
            Use only the exact data supplied by the user or the preceding SqlAgent tool results.
            You have no database tools. If values, units or categories are missing, or the source is
            truncated/failed, explain what is missing instead of inventing a chart. Preserve row order,
            numeric values and nulls; do not turn unknown values into zero. Include metric, unit, time
            range and relevant filters in title/subtext. Output valid JSON without JavaScript functions.
            """)
    @UserMessage("""
            Generate an Apache ECharts chart configuration for: {{query}}
            Response format: ```echarts
            {valid JSON ECharts configuration}
            ```
            """)
    @Agent("Draw ECharts from data already supplied by the user or SqlAgent. Has no database access. Requires exact rows, units and filters; never invents query results.")
    String generateChart(@V("query") String query);
}
