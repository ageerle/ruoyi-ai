package org.ruoyi.ipd.service.channel;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationChannelHandler;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 邮件（EMAIL）通道处理器 真实实现（W20-B：W15-C 中间件化 EMAIL 通道升级）。
 *
 * <p>职责：
 * <ul>
 *   <li>通过 Spring Boot 的 {@link JavaMailSender} 发送 MIME 邮件（UTF-8 + HTML）</li>
 *   <li>支持简单的 i18n 模板渲染：zh-CN / en-US 两套主题/正文模板</li>
 *   <li>Person 表无 email 字段，按 {@code ipd.notification.email.receiver-domain} 合成
 *       {@code person-{receiverId}@{domain}}（本地开发可改 domain）</li>
 *   <li>SMTP 异常 → 抛 {@link NotificationDispatchException}，由 dispatcher 进入退避重试</li>
 *   <li>{@code ipd.notification.email.enabled=false}（默认）时降级为日志发送，便于本地无 SMTP 环境</li>
 * </ul>
 *
 * <p>约束：
 * <ul>
 *   <li>不修改 {@link AsyncNotificationDispatcher} / {@link InAppChannelHandler}</li>
 *   <li>失败语义：抛 {@link NotificationDispatchException}（继承自 RuntimeException），由 dispatcher
 *       走退避重试 + DEAD 死信（与既有 stub 路径同）</li>
 *   <li>{@code from} 为空时退化为 {@code spring.mail.username}</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ipd.notification", name = "email", matchIfMissing = true)
public class EmailChannelHandler implements NotificationChannelHandler {

    /** stub 行为日志标记（与旧 stub sentinel 对齐，便于测试断言） */
    public static final String STUB_SENTINEL = "email-stub-disabled";

    private final JavaMailSender mailSender;

    @Value("${ipd.notification.email.from:${spring.mail.username:noreply@ipd.local}}")
    private String from;

    @Value("${ipd.notification.email.receiver-domain:ipd.local}")
    private String receiverDomain;

    @Value("${ipd.notification.email.enabled:false}")
    private boolean enabled;

    @Value("${ipd.notification.email.encoding:UTF-8}")
    private String encoding;

    @Value("${ipd.notification.email.subject-prefix:[IPD]}")
    private String subjectPrefix;

    @Autowired
    public EmailChannelHandler(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void deliver(NotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            throw new NotificationDispatchException("EMAIL 事件缺 receiverId", new IllegalArgumentException("receiverId required"));
        }
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new NotificationDispatchException("EMAIL 事件缺 title", new IllegalArgumentException("title required"));
        }

        String recipient = buildRecipient(event.getReceiverId());
        String locale = resolveLocale(event.getLocale());
        String subject = renderSubject(event.getTitle(), locale);
        String body = renderBody(event, locale);

        if (!enabled) {
            log.info("[EMAIL-STUB-DISABLED] enabled=false 仅日志；eventId={} receiver={} locale={} subject={} bodyLen={}",
                event.getId(), recipient, locale, subject, body.length());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, encoding);
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML
            mailSender.send(message);
            log.info("[EMAIL-SENT] eventId={} receiver={} locale={} subject={}",
                event.getId(), recipient, locale, subject);
        } catch (MailException | MessagingException e) {
            log.warn("[EMAIL-FAIL] eventId={} receiver={} err={}",
                event.getId(), recipient, e.getMessage());
            throw new NotificationDispatchException("EMAIL 发送失败：" + recipient, e);
        }
    }

    /** 合成收件人地址：person-{receiverId}@{receiverDomain} */
    String buildRecipient(Long receiverId) {
        return "person-" + receiverId + "@" + receiverDomain;
    }

    /** 解析 locale；null/blank 走 zh-CN 默认；非法 tag 也走默认 */
    String resolveLocale(String localeCode) {
        if (localeCode == null || localeCode.isBlank()) {
            return "zh-CN";
        }
        try {
            Locale tag = Locale.forLanguageTag(localeCode.replace('_', '-'));
            if (tag == null || tag.getLanguage().isEmpty()) {
                return "zh-CN";
            }
            return localeCode;
        } catch (RuntimeException ex) {
            return "zh-CN";
        }
    }

    /** 渲染主题：subjectPrefix + 原 title（i18n 化留给后续卡） */
    String renderSubject(String title, String locale) {
        return subjectPrefix + " " + title;
    }

    /** 渲染正文：HTML 模板（按 locale 切换文案）；W20-B 仅落 2 套极简骨架 */
    String renderBody(NotificationEvent event, String locale) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"").append(encoding).append("\"/></head><body>");
        if ("en-US".equalsIgnoreCase(locale)) {
            sb.append("<h2>").append(escape(event.getTitle())).append("</h2>");
            sb.append("<p>").append(escape(event.getContent())).append("</p>");
            sb.append("<p><small>Event: ").append(escape(event.getEventType())).append("</small></p>");
        } else {
            // 默认 zh-CN
            sb.append("<h2>").append(escape(event.getTitle())).append("</h2>");
            sb.append("<p>").append(escape(event.getContent())).append("</p>");
            sb.append("<p><small>事件类型：").append(escape(event.getEventType())).append("</small></p>");
        }
        if (event.getActionUrl() != null && !event.getActionUrl().isBlank()) {
            sb.append("<p><a href=\"").append(escape(event.getActionUrl())).append("\">前往处理</a></p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /** HTML escape（防 XSS；W20-B 极简实现） */
    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    /** 仅供测试注入：覆盖 from/receiverDomain/enabled（暴露给同模块测试用） */
    public void applyOverrides(String from, String receiverDomain, boolean enabled, String encoding, String subjectPrefix) {
        if (from != null) this.from = from;
        if (receiverDomain != null) this.receiverDomain = receiverDomain;
        this.enabled = enabled;
        if (encoding != null) this.encoding = encoding;
        if (subjectPrefix != null) this.subjectPrefix = subjectPrefix;
    }

    /** 用于测试断言 */
    boolean isEnabled() { return enabled; }
    String getFrom() { return from; }
    String getReceiverDomain() { return receiverDomain; }

    /** 用于接收 charset 校验 */
    static java.nio.charset.Charset parseCharset(String name) {
        try { return java.nio.charset.Charset.forName(name); }
        catch (Exception e) { return StandardCharsets.UTF_8; }
    }
}