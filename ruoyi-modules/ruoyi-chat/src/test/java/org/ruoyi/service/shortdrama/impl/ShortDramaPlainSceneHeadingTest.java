package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaPlainSceneHeadingTest {
    @Test void recognizesOurOwnPlainScreenplayWithoutSplittingDialogueOrDroppingText() {
        String script = "《逐风昆仑》第一集：一足定昆仑\n\n一　云梦泽上空。晨。\n飞廉：谁先到谁赢。\n\n二　云梦泽侧。接上。\n飞廉：你先站稳再说。\n\n三　窄口外乱石坡。接上。\n夸父追赶。\n\n四　昆仑山腰。近黄昏。\n夸父：先喝。\n第一集完。";
        var scenes = ShortDramaServiceImpl.splitScriptScenes(script);
        assertEquals(4, scenes.size());
        assertTrue(scenes.get(0).startsWith("《逐风昆仑》"));
        assertTrue(scenes.get(1).startsWith("二　云梦泽侧。接上。"));
        assertTrue(scenes.get(3).endsWith("第一集完。"));
        assertEquals(script.replaceAll("\\s", ""), String.join("", scenes).replaceAll("\\s", ""));
    }

    @Test void aFewSentencesRemainOneSceneAndExistingHeadingFormatsStillWork() {
        assertEquals(1, ShortDramaServiceImpl.splitScriptScenes("古人驾风筝滑翔。侧风吹来，他拉索稳住布翼，继续前行。").size());
        assertEquals(2, ShortDramaServiceImpl.splitScriptScenes("外景 山谷 晨\n飞行。\n内景 工坊 夜\n收绳。").size());
        assertEquals(2, ShortDramaServiceImpl.splitScriptScenes("## 一 山谷\n飞行。\n## 二 工坊\n收绳。").size());
        assertEquals(1, ShortDramaServiceImpl.splitScriptScenes("一　山谷上空。日。\n匠人：一、先看风；二、再拉索。\n一只鸟掠过。\n第一集完。").size());
    }
}
