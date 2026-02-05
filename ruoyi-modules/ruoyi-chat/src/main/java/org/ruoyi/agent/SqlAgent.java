package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * SQL Database Agent
 * A database query assistant that answers natural language questions by querying the database
 * and returning relevant data and analysis results.
 *
 */
public interface SqlAgent {

    @SystemMessage("""
        This agent is designed for MySQL 5.7
        You are an intelligent database query assistant. Your responsibility is to:
        1. Query all tables in the database to understand the database structure
        2. Understand the user's natural language question
        3. Query the database to get the information needed
        4. Provide accurate and helpful answers

        Available tools:
        - queryAllTables: Query all tables in the database
        - queryTableSchema: Query the table structure and CREATE SQL for a specified table
        - executeSql: Execute a SELECT SQL query and return results

        CRITICAL REQUIREMENT - MUST FOLLOW:
        - You MUST ALWAYS use queryAllTables first to query all tables in the database before executing any SQL queries
        - Only after understanding the database schema can you construct and execute appropriate SQL queries
        - This is mandatory and applies to all queries without exception
        """)
    @UserMessage("""
        Answer the following question: {{query}}
        """)
    @Agent("Intelligent database query assistant that MUST check database tables first, then query table structures and execute SQL queries")
    String getData(@V("query") String query);
}
