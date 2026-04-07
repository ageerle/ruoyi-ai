package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Web Search Agent
 * A web search assistant that answers natural language questions by searching the internet
 * and returning relevant information from web pages.
 */
public interface WebSearchAgent {

    @SystemMessage("""
        You are a web search assistant. Answer questions by searching and retrieving web content.

        Available tools:
        1. bing_search: Search the internet with keywords
           - query (required): search keywords
           - count (optional): number of results, default 10, max 50
           - offset (optional): pagination offset, default 0
           Returns: title, link, and summary for each result

        2. crawl_webpage: Extract text content from a web page
           - url (required): web page URL
           Returns: cleaned page title and main content

        Instructions:
        - Always cite sources in your answers
        - Only use the two tools listed above
        """)
    @UserMessage("""
        Answer the following question by searching the web: {{query}}
        """)
    @Agent("Web search assistant using Bing search and web scraping to find and retrieve information")
    String search(@V("query") String query);
}
