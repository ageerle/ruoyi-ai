package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mapper.shortdrama.ShortDramaScriptMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Tag("dev")
class ShortDramaMusicServiceTest {
    private ShortDramaMusicService.MusicRequest request(String purpose,String prompt,int seconds) {return new ShortDramaMusicService.MusicRequest("b4c75c77-50af-4330-94fb-88b3c6d23580","suno/chirp-v6",purpose,"灯下",""+prompt,"guqin, restrained strings",seconds,"Male","EDM");}
    @Test void bgmAndLyricsUseMusicContractWithoutTtsText() {
        var bgm=ShortDramaMusicService.payload(request("bgm","",180));assertEquals(true,bgm.get("custom"));assertEquals(true,bgm.get("instrumental"));assertFalse(bgm.containsKey("text"));assertFalse(bgm.containsKey("vocal_gender"));
        var song=ShortDramaMusicService.payload(request("ending","[Verse]\n灯下这一笔",75));assertEquals(false,song.get("instrumental"));assertEquals(false,song.get("auto_lyrics"));assertEquals("Male",song.get("vocal_gender"));assertEquals("[Verse]\n灯下这一笔",song.get("prompt"));
    }
    @Test void invalidParametersFailBeforePaidSubmission() {
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(request("ending","",75)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(request("bgm","",9)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(request("bgm","",361)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(request("theme","字".repeat(3001),75)));
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(request("reference","",75)));
    }
    @Test void keepsBothTracksAndExcludesThumbnail() throws Exception {
        var data=AtlasMediaSupport.OBJECT_MAPPER.readTree("{\"outputs\":[\"https://cdn/a.mp3\",{\"audio_url\":\"https://cdn/b.mp3\"}],\"thumbnail\":\"https://cdn/cover.jpg\"}");assertEquals(List.of("https://cdn/a.mp3","https://cdn/b.mp3"),ShortDramaMusicService.outputs(data));
    }
    @Test void stripsModelProductionNotesFromLyricsAndBoundsStyleBeforeSunoSubmission() {
        var normalized=ShortDramaMusicService.normalizeBrief(new ShortDramaMusicService.Brief("灯下","strings ".repeat(160),"【音乐发展】不要演唱这段解说\n【歌词】\n[Verse 1]\n烛下待天明"),"ending");
        assertEquals("[Verse 1]\n烛下待天明",normalized.prompt());assertTrue(normalized.style().length()<=1000);
        var valid=request("bgm","",180);var tooLong=new ShortDramaMusicService.MusicRequest(valid.requestId(),valid.model(),valid.purpose(),valid.title(),valid.prompt(),"a".repeat(1001),180,"Male","");
        assertThrows(IllegalArgumentException.class,()->ShortDramaMusicService.payload(tooLong));
    }
    @Test void nonOwnerCannotReadTasksOrSubmitMusicOrWriteLyrics() {
        var sounds=mock(ShortDramaSoundService.class);var models=mock(IChatModelService.class);var predictions=mock(AtlasPredictionService.class);var chats=mock(ChatServiceFactory.class);
        doThrow(new IllegalArgumentException("无权限")).when(sounds).owner(1L,2L);
        var service=new ShortDramaMusicService(sounds,models,predictions,chats,mock(ShortDramaScriptMapper.class),mock(ShortDramaStoryboardMapper.class));
        assertThrows(IllegalArgumentException.class,()->service.list(1L,2L));assertThrows(IllegalArgumentException.class,()->service.generate(1L,2L,request("bgm","",180)));assertThrows(IllegalArgumentException.class,()->service.poll(1L,2L,"any"));assertThrows(IllegalArgumentException.class,()->service.write(1L,2L,new ShortDramaMusicService.BriefRequest("doubao","ending","")));verifyNoInteractions(models,predictions,chats);
    }
}
