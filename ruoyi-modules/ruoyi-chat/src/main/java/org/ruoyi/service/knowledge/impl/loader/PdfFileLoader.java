package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@AllArgsConstructor
public class PdfFileLoader implements ResourceLoader {
    private final TextSplitter characterTextSplitter;

    @Override
    public String getContent(InputStream inputStream) {
        PDDocument document = null;
        try {
            document = Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
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
