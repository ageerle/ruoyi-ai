package org.ruoyi.chain.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chain.split.TextSplitter;
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
        String stringBuffer = "";
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            stringBuffer = bufferedReader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return textSplitter.split(content, kid);
    }
}
