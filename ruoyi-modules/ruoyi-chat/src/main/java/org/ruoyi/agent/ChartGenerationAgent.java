package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


public interface ChartGenerationAgent {

    @SystemMessage("""
            You are a chart generation specialist. Your only task is to generate Apache ECharts
            chart configurations. Respond with ONLY the ECharts configuration in ```echarts markdown
            code block format. Do not include any explanations, descriptions, or other content.
            """)
    @UserMessage("""
            Generate an Apache ECharts chart configuration for: {{query}}
            Response format: ```echarts
            {valid JSON ECharts configuration}
            ```
            """)
    @Agent("Generate Apache ECharts chart configurations only.")
    String generateChart(@V("query") String query);
}
