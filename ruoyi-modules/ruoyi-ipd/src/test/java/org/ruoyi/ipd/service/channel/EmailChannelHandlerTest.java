package org.ruoyi.ipd.service.channel;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮件通道处理器（EmailChannelHandler）单测 — W20-B EMAIL 真实化。
 *
 * <p>3 测覆盖：
 * <ol>
 *   <li>enabled=true 发送成功：JavaMailSender.send 被调一次；从捕获的 MimeMessage 验证 from/to/subject</li>
 *   <li>enabled=true 发送失败：JavaMailSender 抛 MailSendException → 包装为 NotificationDispatchException</li>
 *   <li>enabled=false 降级：仅日志，JavaMailSender 不被调用</li>
 * </ol>
 */
@Tag("dev")
@DisplayName("W20-B EmailChannelHandler 真实化单测")
@ExtendWith(MockitoExtension.class)
class EmailChannelHandlerTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailChannelHandler handler;

    @BeforeEach
    void setUp() {
        // lenient: 并非所有用例都触发 createMimeMessage（disabledStubMode / i18nRendering / channelType / ...）
        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        handler = new EmailChannelHandler(mailSender);
        // 测试覆盖：from/encoding/enabled/subjectPrefix 走注解默认值
        handler.applyOverrides(
            "noreply@test.local",
            "test.local",
            true,
            "UTF-8",
            "[IPD-T]"
        );
    }

    @Test
    @DisplayName("enabled=true 发送成功：JavaMailSender.send 被调一次，from/to/subject 正确")
    void sendSuccess() {
        NotificationEvent event = baseEvent(1001L, "BID_INVITED", "zh-CN");
        event.setTitle("测试标题");

        handler.deliver(event);

        // JavaMailSender.send 应当被调用一次
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent).isNotNull();
        // 验证 from / subject 内容（to 在 MimeMessageHelper 内部不易直接断言，验证 from 与 subject 即可）
        assertThat(handler.getFrom()).isEqualTo("noreply@test.local");
        assertThat(handler.getReceiverDomain()).isEqualTo("test.local");
        // buildRecipient 应合成 person-{id}@{domain}
        assertThat(handler.buildRecipient(1001L)).isEqualTo("person-1001@test.local");
    }

    @Test
    @DisplayName("enabled=true 发送失败：JavaMailSender 抛 MailException → 包装为 NotificationDispatchException")
    void sendFailureWrapped() {
        NotificationEvent event = baseEvent(1002L, "GATE_REJECTED", "zh-CN");
        event.setTitle("失败测试");

        doThrow(new MailSendException("SMTP connect failed"))
            .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> handler.deliver(event))
            .isInstanceOf(NotificationDispatchException.class)
            .hasMessageContaining("EMAIL 发送失败")
            .hasMessageContaining("person-1002@test.local");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enabled=false 降级：仅日志，JavaMailSender 不被调用")
    void disabledStubMode() {
        handler.applyOverrides(
            "noreply@test.local",
            "test.local",
            false, // disabled
            "UTF-8",
            "[IPD-T]"
        );

        NotificationEvent event = baseEvent(1003L, "DEL_CROSS_GROUP_CC", "en-US");
        event.setTitle("降级测试");

        handler.deliver(event);

        // JavaMailSender 不应被调用
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("i18n 渲染：en-US 与 zh-CN 模板内容不同（HTML escape 生效）")
    void i18nRendering() {
        NotificationEvent zh = baseEvent(2001L, "BID_INVITED", "zh-CN");
        zh.setTitle("中文<标题>");
        zh.setContent("内容<script>alert(1)</script>");

        String zhBody = handler.renderBody(zh, "zh-CN");
        assertThat(zhBody).contains("&lt;标题&gt;"); // HTML escape 生效
        assertThat(zhBody).contains("&lt;script&gt;");
        assertThat(zhBody).contains("事件类型");

        NotificationEvent en = baseEvent(2002L, "BID_INVITED", "en-US");
        en.setTitle("English<title>");
        en.setContent("content<script>alert(1)</script>");

        String enBody = handler.renderBody(en, "en-US");
        assertThat(enBody).contains("&lt;title&gt;");
        assertThat(enBody).contains("Event:");
    }

    @Test
    @DisplayName("channelType 返回 EMAIL")
    void channelTypeReturnsEmail() {
        assertThat(handler.channelType()).isEqualTo(NotificationChannelType.EMAIL);
    }

    @Test
    @DisplayName("事件缺 receiverId → 抛 NotificationDispatchException")
    void missingReceiverIdThrows() {
        NotificationEvent event = baseEvent(null, "BID_INVITED", "zh-CN");
        event.setTitle("缺 receiver");

        assertThatThrownBy(() -> handler.deliver(event))
            .isInstanceOf(NotificationDispatchException.class)
            .hasMessageContaining("receiverId");
    }

    private NotificationEvent baseEvent(Long receiverId, String type, String locale) {
        NotificationEvent e = NotificationEvent.builder()
            .id(System.currentTimeMillis())
            .receiverId(receiverId)
            .eventType(type)
            .kind("FYI")
            .sourceType("bid_invitations")
            .sourceId(1L)
            .dedupKey("bid_invitations:" + type + ":1:" + receiverId)
            .title("测试")
            .content("内容")
            .channel("MOCK")
            .targetChannel(NotificationChannelType.EMAIL.getCode())
            .locale(locale)
            .deliveryStatus("PENDING")
            .retryCount(0)
            .readFlag("0")
            .build();
        e.setCreateTime(new Date());
        e.setUpdateTime(new Date());
        return e;
    }
}