package org.ruoyi.service.knowledge.impl.split;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.ruoyi.service.knowledge.impl.loader.CodeFileLoader;
import org.ruoyi.service.knowledge.impl.loader.MarkDownFileLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class RagTextSplitterRegressionTest {

    @Test
    void allSplittersHonorLiteralSeparatorAndStrictMaximum() {
        List.of(new CharacterTextSplitter(), new MarkdownTextSplitter(),
            new CodeTextSplitter(), new ExcelTextSplitter()).forEach(splitter -> {
            for (String separator : List.of("|", ".", "*", "<CUT>", "\n---\n")) {
                String content = "alpha" + separator + "b".repeat(35);
                List<String> chunks = splitter.split(content, config(separator, 12, 0));
                assertTrue(chunks.size() >= 4, splitter.getClass().getSimpleName());
                assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 12));
                assertTrue(chunks.stream().noneMatch(chunk -> chunk.contains(separator)));
            }
        });
    }

    @Test
    void zeroOverlapIsRespectedAndConfiguredOverlapIsExact() {
        CharacterTextSplitter splitter = new CharacterTextSplitter();
        assertEquals(List.of("abcdefghij", "klmnopqrst"),
            splitter.split("abcdefghijklmnopqrst", config(null, 10, 0)));
        List<String> overlap = splitter.split("abcdefghijklmnop", config(null, 10, 3));
        assertEquals("hij", overlap.get(0).substring(7));
        assertTrue(overlap.get(1).startsWith("hij"));
        assertTrue(overlap.stream().allMatch(chunk -> chunk.length() <= 10));
    }

    @Test
    void markdownKeepsHeadingsAndIgnoresHeadingsInsideFences() {
        MarkdownTextSplitter splitter = new MarkdownTextSplitter();
        String markdown = "# Real\nintro\n```md\n# not a heading\n```\nTitle\n=====\nbody";
        List<String> chunks = splitter.split(markdown, config(null, 200, 0));
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("# Real"));
        assertTrue(chunks.get(0).contains("# not a heading"));
        assertTrue(chunks.get(0).contains("Title\n====="));
    }

    @Test
    void invalidConfigurationFailsClearly() {
        assertThrows(ServiceException.class, () -> config(null, 0, 0));
        assertThrows(ServiceException.class, () -> config(null, 10, 10));
        assertThrows(ServiceException.class, () -> config(null, 10, -1));
    }

    @Test
    void loaderFactoryNormalizesSuffixAndUsesFormatSpecificLoader() {
        ResourceLoaderFactory factory = new ResourceLoaderFactory(new CharacterTextSplitter(),
            new CodeTextSplitter(), new MarkdownTextSplitter(), new ExcelTextSplitter());
        assertInstanceOf(MarkDownFileLoader.class, factory.getLoaderByFileType(" .MD "));
        assertInstanceOf(CodeFileLoader.class, factory.getLoaderByFileType(".JAVA"));
    }

    private DocumentSplitConfig config(String separator, int size, int overlap) {
        return new DocumentSplitConfig(separator, size, overlap, "md");
    }
}
