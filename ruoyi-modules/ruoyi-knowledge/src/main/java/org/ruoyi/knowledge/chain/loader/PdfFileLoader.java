package org.ruoyi.knowledge.chain.loader;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.ruoyi.knowledge.chain.split.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@AllArgsConstructor
public class PdfFileLoader implements ResourceLoader{
    private final TextSplitter characterTextSplitter;
    @Override
    public String getContent(InputStream inputStream) {
        PDDocument document = null;
        try {
            document = PDDocument.load(inputStream);
            PDFTextStripper textStripper = new PDFTextStripper();
            String content = textStripper.getText(document);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return characterTextSplitter.split(content, kid);
    }
}
