package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVoiceContractTest {
    private ShortDramaCharacter actor(long id,String name,String aliases) {var actor=new ShortDramaCharacter();actor.setId(id);actor.setName(name);actor.setAliases(aliases);return actor;}
    private ShortDramaStoryboard shot(String source,String continuity) {var shot=new ShortDramaStoryboard();shot.setSourceText(source);shot.setContinuityJson(continuity);return shot;}
    @Test void resolvesOffscreenAndAliasesWithoutIncludingSilentVisibleCast() {
        var a=actor(1,"阿青","青姐");var b=actor(2,"阿白",null);var silent=actor(3,"路人",null);
        var speakers=ShortDramaVoiceContract.resolve(shot("阿青（画外）：\"先等等。\"\n阿白：「明白。」\n青姐：「我马上回来。」\n路人安静看着。\n画面文字：「入夜」。","{}"),List.of(a,b,silent));
        assertEquals(List.of(a,b),speakers.actors());assertTrue(speakers.issues().isEmpty());
    }
    @Test void explicitEmptyMeansNoDialogueAndForeignIdsCannotBind() {
        var a=actor(1,"阿青",null);
        assertTrue(ShortDramaVoiceContract.resolve(shot("阿青：「上一段的对白。」","{\"voice_speakers\":[]}"),List.of(a)).actors().isEmpty());
        assertThrows(IllegalArgumentException.class,()->ShortDramaVoiceContract.resolve(shot("","{\"voice_speakers\":[\"99\"]}"),List.of(a)));
    }
    @Test void ambiguousLegacyDialogueRequiresReviewInsteadOfChoosingTheNearestName() {
        var result=ShortDramaVoiceContract.resolve(shot("阿青看着阿白，轻蔑地说：“不用了。”","{}"),List.of(actor(1,"阿青",null),actor(2,"阿白",null)));
        assertFalse(result.issues().isEmpty());assertTrue(result.actors().isEmpty());
    }
    @Test void unquotedSpeakerLabelsAreBoundWhileStageDirectionsRemainSilent() {
        var actor=actor(1,"阿青",null);
        var result=ShortDramaVoiceContract.resolve(shot("阿青（画外音）：先等等。\n画面：阿白回头，沉默看着窗外。","{}"),List.of(actor,actor(2,"阿白",null)));
        assertEquals(List.of(actor),result.actors());assertTrue(result.issues().isEmpty());
    }
    @Test void validatesCombinedRoleAndManualReferencesAndModelCapability() {
        String mini="bytedance/seedance-2.0-mini/reference-to-video",v25="bytedance/seedance-2.5/reference-to-video";
        assertDoesNotThrow(()->ShortDramaVoiceContract.validateReferences(mini,List.of(3.0,4.0,5.0)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaVoiceContract.validateReferences(mini,List.of(2.0,2.0,2.0,2.0)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaVoiceContract.validateReferences(mini,List.of(8.0,8.0)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaVoiceContract.validateReferences(mini,List.of(1.0)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaVoiceContract.validateReferences("other-model",List.of(3.0)));
        assertDoesNotThrow(()->ShortDramaVoiceContract.validateReferences(v25,List.of(4.0,4.0,4.0,4.0)));
    }
}
