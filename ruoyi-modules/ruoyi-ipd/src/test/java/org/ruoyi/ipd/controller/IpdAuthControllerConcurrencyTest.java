/**
 * IpdAuthController 并发守卫（[CONSISTENCY-18] 2026-09-06）。
 * - logout 幂等：重复 logout 不抛
 * - refresh 守卫：currentPerson 缓存后 logout，缓存的 Person 用于 login 不抛
 */
package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.service.IpdAuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class IpdAuthControllerConcurrencyTest {

    @Mock
    private IpdAuthSession session;
    @Mock
    private org.ruoyi.ipd.service.IpdAuthService authService;

    @InjectMocks
    private IpdAuthController controller;

    @Test
    @DisplayName("[CONSISTENCY-18] logout 幂等：tokenValue=null 时不调 logout")
    void logoutIdempotent() {
        when(session.tokenValue()).thenReturn(null);
        controller.logout();
        verify(session, never()).logout();
    }

    @Test
    @DisplayName("[CONSISTENCY-18] logout 正常路径：tokenValue!=null 调一次 logout")
    void logoutOnce() {
        when(session.tokenValue()).thenReturn("t-1");
        controller.logout();
        verify(session, times(1)).logout();
    }

    @Test
    @DisplayName("[CONSISTENCY-18] refresh 守卫：缓存 Person 后 logout 用缓存发新 token 不依赖 currentPerson")
    void refreshCache() {
        Person person = new Person();
        person.setId(100L);
        when(session.currentPerson()).thenReturn(person);
        when(session.tokenValue()).thenReturn("t-old").thenReturn(null);
        when(session.login(person)).thenReturn("t-new");
        when(session.timeout()).thenReturn(900L);
        when(authService.scopeOf(person)).thenReturn(IpdAuthService.Scope.FULL);
        controller.refresh();
        // currentPerson 只调一次（logout 后不依赖）
        verify(session, times(1)).currentPerson();
        verify(session, times(1)).login(person);
    }
}
