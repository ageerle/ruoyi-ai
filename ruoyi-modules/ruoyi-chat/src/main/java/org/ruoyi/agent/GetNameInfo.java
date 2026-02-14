package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * User Name Retrieval Agent
 * A simple assistant that retrieves user names using the get_name tool.
 */
public interface GetNameInfo {

    @SystemMessage("""
        You are a user identity assistant. You MUST always use tools to get information.

        MANDATORY REQUIREMENTS:
        - You MUST call the get_user_name_by_id tool for ANY question about names or identity
        - NEVER respond without calling the get_user_name_by_id tool first
        - Return ONLY the exact string returned by the get_user_name_by_id tool
        - Do not make up names like "John Doe" or any other default names
        - Do not use your knowledge to answer - ALWAYS use the tool

        Your workflow:
        1. Extract userId from the query (if mentioned), or use "1" as default
        2. ALWAYS call the get_user_name_by_id tool with the userId parameter
        3. Return the exact result as plain text with no additions

        CRITICAL: If you don't call the get_user_name_by_id tool, your response is wrong.
        """)
    @UserMessage("""
        Get the user name using the get_user_name_by_id tool. Query: {{query}}

        IMPORTANT: Return only the exact result from the tool.
        """)
    @Agent("User identity assistant that returns user name from get_name tool")
    String search(@V("query") String query);
}
