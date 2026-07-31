package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.ruoyi.service.knowledge.TextSplitter;
import org.ruoyi.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class TextFileLoader implements ResourceLoader {
    private final TextSplitter textSplitter;

    @Override
    public String getContent(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new ServiceException("读取文本文件失败", e);
        }
    }

    @Override
    public List<String> getChunkList(String content, DocumentSplitConfig config) {
        return textSplitter.split(content, config);
    }
}
