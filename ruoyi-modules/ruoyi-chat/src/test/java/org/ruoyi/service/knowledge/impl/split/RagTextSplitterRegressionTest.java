package org.ruoyi.service.knowledge.impl.split;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class RagTextSplitterRegressionTest {

    private static IKnowledgeInfoService knowledgeService(String separator, long blockSize, long overlap) {
        IKnowledgeInfoService service = mock(IKnowledgeInfoService.class);
        KnowledgeInfoVo info = new KnowledgeInfoVo();
        info.setSeparator(separator);
        info.setTextBlockSize(blockSize);
        info.setOverlapChar(overlap);
        when(service.queryById(1L)).thenReturn(info);
        return service;
    }

    @Test
    void characterSplitterTreatsRegexMetacharactersLiterally() {
        CharacterTextSplitter pipe = new CharacterTextSplitter(knowledgeService("|", 1000, 50));
        assertEquals(List.of("alpha", "beta", "gamma"), pipe.split("alpha|beta|gamma", "1"));

        CharacterTextSplitter dot = new CharacterTextSplitter(knowledgeService(".", 1000, 50));
        assertEquals(List.of("alpha", "beta", "gamma"), dot.split("alpha.beta.gamma", "1"));

        CharacterTextSplitter star = new CharacterTextSplitter(knowledgeService("*", 1000, 50));
        assertEquals(List.of("alpha", "beta", "gamma"), star.split("alpha*beta*gamma", "1"));
    }

    @Test
    void markdownSplitterReturnsNonEmptyBoundedChunks() {
        MarkdownTextSplitter splitter = new MarkdownTextSplitter(knowledgeService(null, 40, 5));
        String markdown = "# Title\nintro text\n## Details\n" + "detail ".repeat(20);

        List<String> chunks = splitter.split(markdown, "1");

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().noneMatch(String::isBlank));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 50),
            "window size may include overlap on both sides");
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("# Title")));
    }

    @Test
    void codeSplitterReturnsNonEmptyChunksAndPreservesContent() {
        CodeTextSplitter splitter = new CodeTextSplitter(knowledgeService(null, 45, 5));
        String code = "class A {\n  void a() {}\n}\n\nclass B {\n" + "  int value = 1;\n".repeat(8) + "}";

        List<String> chunks = splitter.split(code, "1");

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().noneMatch(String::isBlank));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("class A")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.contains("class B") || chunk.contains("int value")));
    }

    @Test
    void splitterSupportHandlesEmptyAndOversizedSections() {
        assertTrue(SplitterSupport.mergeAndSplit(new String[]{"", "  "}, 20, 3).isEmpty());

        List<String> chunks = SplitterSupport.mergeAndSplit(
            new String[]{"short", "x".repeat(55)}, 20, 3);

        assertEquals("short", chunks.get(0));
        assertTrue(chunks.size() >= 4);
        assertTrue(chunks.stream().noneMatch(String::isBlank));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 26));
    }
}
