package org.ruoyi.workflow.workflow.node.mailSend;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
public class MailSendNode extends AbstractWfNode {

    public MailSendNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    public NodeProcessResult onProcess() {
        try {
            MailSendNodeConfig config = checkAndGetConfig(MailSendNodeConfig.class);
            List<NodeIOData> inputs = state.getInputs();

            // 安全获取模板（使用 defaultString 避免 null）
            String subjectTemplate = StringUtils.defaultString(config.getSubject());
            String contentTemplate = StringUtils.defaultString(config.getContent());
            String toMailsTemplate = StringUtils.defaultString(config.getToMails());
            String ccMailsTemplate = StringUtils.defaultString(config.getCcMails());

            // 渲染收件人和主题
            String toMails = WorkflowUtil.renderTemplate(toMailsTemplate, inputs);
            String ccMails = WorkflowUtil.renderTemplate(ccMailsTemplate, inputs);
            String subject = WorkflowUtil.renderTemplate(subjectTemplate, inputs);

            // 内容：优先使用配置的内容模板，否则使用 output 或 input 参数
            String content;
            if (StringUtils.isNotBlank(contentTemplate)) {
                content = WorkflowUtil.renderTemplate(contentTemplate, inputs);
            } else {
                // 优先使用 output，如果没有则使用 input
                content = inputs.stream()
                        .filter(item -> "output".equals(item.getName()))
                        .map(NodeIOData::valueToString)
                        .findFirst()
                        .orElseGet(() -> inputs.stream()
                                .filter(item -> "input".equals(item.getName()))
                                .map(NodeIOData::valueToString)
                                .findFirst()
                                .orElse(""));
            }

            // 将换行符转换为 HTML 换行
            if (StringUtils.isNotBlank(content)) {
                content = content.replace("\n", "<br>");
            } else {
                content = ""; // 安全兜底
            }

            // 校验必要字段
            if (config.getSender() == null) {
                throw new IllegalArgumentException("发件人配置（sender）不能为空");
            }
            if (StringUtils.isBlank(toMails)) {
                throw new IllegalArgumentException("收件人邮箱（to_mails）不能为空或未解析出有效值");
            }

            // 创建邮件发送器
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSender().getMail(), config.getSender().getName());

            // 设置收件人
            String[] toArray = Arrays.stream(toMails.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .toArray(String[]::new);
            if (toArray.length == 0) {
                throw new IllegalArgumentException("收件人邮箱列表为空");
            }
            helper.setTo(toArray);

            // 设置抄送（如有）
            if (StringUtils.isNotBlank(ccMails)) {
                String[] ccArray = Arrays.stream(ccMails.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .toArray(String[]::new);
                if (ccArray.length > 0) {
                    helper.setCc(ccArray);
                }
            }

            // 设置主题和内容（支持 HTML）
            helper.setSubject(subject);
            helper.setText(content, true);

            // 发送
            mailSender.send(message);
            log.info("Email sent successfully to: {}", toMails);

            // 构造输出：统一输出为 output 参数
            List<NodeIOData> outputs = new java.util.ArrayList<>();

            // 优先使用 output，如果没有则使用 input（但重命名为 output）
            inputs.stream()
                    .filter(item -> "output".equals(item.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            outputs::add,
                            () -> inputs.stream()
                                    .filter(item -> "input".equals(item.getName()))
                                    .findFirst()
                                    .ifPresent(inputParam -> {
                                        String title = inputParam.getContent() != null && inputParam.getContent().getTitle() != null
                                                ? inputParam.getContent().getTitle() : "";
                                        NodeIOData outputParam = NodeIOData.createByText("output", title, inputParam.valueToString());
                                        outputs.add(outputParam);
                                    })
                    );

            return NodeProcessResult.builder().content(outputs).build();

        } catch (Exception e) {
            log.error("Failed to send email in node: {}", node.getId(), e);
            // 异常时也统一输出为 output 参数，添加错误信息
            List<NodeIOData> errorOutputs = new java.util.ArrayList<>();

            state.getInputs().stream()
                    .filter(item -> "output".equals(item.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            errorOutputs::add,
                            () -> state.getInputs().stream()
                                    .filter(item -> "input".equals(item.getName()))
                                    .findFirst()
                                    .ifPresent(inputParam -> {
                                        String title = inputParam.getContent() != null && inputParam.getContent().getTitle() != null
                                                ? inputParam.getContent().getTitle() : "";
                                        NodeIOData outputParam = NodeIOData.createByText("output", title, inputParam.valueToString());
                                        errorOutputs.add(outputParam);
                                    })
                    );

            errorOutputs.add(NodeIOData.createByText("error", "mail", e.getMessage()));
            return NodeProcessResult.builder().content(errorOutputs).build();
        }
    }

    private JavaMailSender createMailSender(MailSendNodeConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtp().getHost());
        sender.setPort(config.getSmtp().getPort());
        sender.setUsername(config.getSender().getMail());
        sender.setPassword(config.getSender().getPassword());

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true"); // QQ 邮箱 465 必须开 SSL
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        sender.setJavaMailProperties(props);

        return sender;
    }
}
