package org.ruoyi.chain.loader;

import lombok.AllArgsConstructor;
import org.ruoyi.chain.loader.ResourceLoader;
import org.ruoyi.chain.loader.TextFileLoader;
import org.ruoyi.chain.split.CharacterTextSplitter;
import org.ruoyi.chain.split.CodeTextSplitter;
import org.ruoyi.chain.split.MarkdownTextSplitter;
import org.ruoyi.chain.split.TokenTextSplitter;
import org.ruoyi.knowledge.constant.FileType;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ResourceLoaderFactory {
    private final CharacterTextSplitter characterTextSplitter;
    private final CodeTextSplitter codeTextSplitter;
    private final MarkdownTextSplitter markdownTextSplitter;
    private final TokenTextSplitter tokenTextSplitter;
    public ResourceLoader getLoaderByFileType(String fileType){
        if (FileType.isTextFile(fileType)){
            return new TextFileLoader(characterTextSplitter);
        } else if (FileType.isWord(fileType)) {
            return new WordLoader(characterTextSplitter);
        } else if (FileType.isPdf(fileType)) {
            return new PdfFileLoader(characterTextSplitter);
        } else if (FileType.isMdFile(fileType)) {
            return new MarkDownFileLoader(markdownTextSplitter);
        }else if (FileType.isCodeFile(fileType)) {
            return new CodeFileLoader(codeTextSplitter);
        }else {
            return new TextFileLoader(characterTextSplitter);
        }
    }
}
