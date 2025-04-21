package org.ruoyi.chain.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chain.split.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class TokenTextSplitter implements TextSplitter {
    @Override
    public List<String> split(String content, String kid) {
        return null;
    }
}
