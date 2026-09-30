package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.bo.shortdrama.ShortDramaRevisionBo;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaRevisionServiceTest {
    @Test void unauthorizedAndStaleRevisionNeverWrite() {
        var projects = mock(ShortDramaProjectMapper.class);
        var scripts = mock(ShortDramaScriptMapper.class);
        var boards = mock(ShortDramaStoryboardMapper.class);
        var tx = mock(TransactionTemplate.class);
        doAnswer(call -> { ((Consumer<TransactionStatus>)call.getArgument(0)).accept(mock(TransactionStatus.class)); return null; }).when(tx).executeWithoutResult(any());
        var service = new ShortDramaRevisionService(projects, scripts, boards,
            mock(ShortDramaCharacterMapper.class), mock(ShortDramaLocationMapper.class), mock(ShortDramaVisualAssetMapper.class), mock(ShortDramaVisualAssetService.class), mock(IShortDramaService.class), tx);
        var p = new ShortDramaProject(); p.setId(1L); p.setUserId(7L); p.setStatus("storyboard_ready");
        when(projects.selectById(1L)).thenReturn(p);
        var r = new ShortDramaRevisionBo(); r.setScriptId(2L); r.setExpectedScriptText("old");
        assertThrows(IllegalArgumentException.class, () -> service.apply(1L, r, 8L));
        verifyNoInteractions(scripts, boards);
        var s = new ShortDramaScript(); s.setId(2L); s.setProjectId(1L); s.setScriptText("newer user edit");
        when(scripts.selectById(2L)).thenReturn(s);
        assertThrows(IllegalStateException.class, () -> service.apply(1L, r, 7L));
        verifyNoInteractions(boards);
        verify(scripts, never()).updateById(any(ShortDramaScript.class));
    }
}
