package org.ruoyi.chat.service.coding.harness.prompt;

public record HarnessPromptBundle(
    String systemPrompt,
    String staticPrefixSha256,
    String completePromptSha256,
    String version
) {
}
