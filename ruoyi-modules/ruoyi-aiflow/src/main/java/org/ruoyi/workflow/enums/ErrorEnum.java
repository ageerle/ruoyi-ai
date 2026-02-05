package org.ruoyi.workflow.enums;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    SUCCESS("00000", "成功"),
    A_URL_NOT_FOUND("A0001", "地址不存在"),
    A_PARAMS_ERROR("A0002", "参数校验不通过"),
    A_REQUEST_TOO_MUCH("A0003", "访问次数太多"),
    A_LOGIN_ERROR("A0004", "登陆失败，账号或密码错误"),
    A_LOGIN_ERROR_MAX("A0005", "失败次数太多，请输入验证码重试"),
    A_LOGIN_CAPTCHA_ERROR("A0006", "验证码不正确"),
    A_USER_NOT_EXIST("A0007", "用户不存在"),
    A_CONVERSATION_NOT_EXIST("A0008", "对话不存在"),
    A_IMAGE_NUMBER_ERROR("A0009", "图片数量不对"),
    A_IMAGE_SIZE_ERROR("A0010", "图片尺寸不对"),
    A_FILE_NOT_EXIST("A0011", "文件不存在"),
    A_DRAWING("A0012", "作图还未完成"),
    A_USER_EXIST("A0013", "账号已经存在，请使用账号密码登录"),
    A_FIND_PASSWORD_CODE_ERROR("A0014", "重置码已过期或不存在"),
    A_USER_WAIT_CONFIRM("A0015", "用户未激活"),
    A_USER_NOT_AUTH("A0016", "用户无权限"),
    A_DATA_NOT_FOUND("A0017", "数据不存在"),
    A_UPLOAD_FAIL("A0018", "上传失败"),
    A_QA_ASK_LIMIT("A0019", "请求次数太多"),
    A_QA_ITEM_LIMIT("A0020", "知识点生成已超额度"),
    A_CONVERSATION_EXIST("A0021", "会话(角色)已存在"),
    A_MODEL_NOT_FOUND("A0022", "模型不存在"),
    A_MODEL_ALREADY_EXIST("A0023", "模型已存在"),
    A_CONVERSATION_NOT_FOUND("A0024", "会话(角色)找不到"),
    A_AI_IMAGE_NOT_FOUND("A0024", "图片找不到"),
    A_ENABLE_MODEL_NOT_FOUND("A0025", "没有可用的模型"),
    A_DOC_INDEX_DOING("A0026", "文档索引正在进行中，请稍后重试"),
    A_PRESET_CONVERSATION_NOT_EXIST("A0027", "预设会话或角色不存在"),
    A_CONVERSATION_TITLE_EXIST("A0028", "会话(角色)标题已存在"),
    A_AI_IMAGE_NO_AUTH("A0029", "无权限查看该图片"),
    A_USER_NOT_FOUND("A0030", "用户不存在"),
    A_ACTIVE_CODE_INVALID("A0031", "激活码已失效"),
    A_OLD_PASSWORD_INVALID("A0032", "原密码不正确"),
    A_OPT_TOO_FREQUENTLY("A0032", "操作太频繁"),
    A_DRAW_NOT_FOUND("A00033", "绘图记录找不到"),
    A_WF_NOT_FOUND("A00034", "工作流找不到"),
    A_WF_DISABLED("A0035", "工作流已停用"),
    A_WF_NODE_NOT_FOUND("A0036", "工作流节点找不到"),
    A_WF_NODE_CONFIG_NOT_FOUND("A0037", "工作流节点配置找不到"),
    A_WF_NODE_CONFIG_ERROR("A0038", "工作流节点配置异常"),
    A_WF_INPUT_INVALID("A0039", "工作流输入参数错误"),
    A_WF_INPUT_MISSING("A0040", "工作流输入缺少参数"),
    A_WF_MULTIPLE_START_NODE("A0041", "多个开始节点"),
    A_WF_START_NODE_NOT_FOUND("A0042", "没有开始节点"),
    A_WF_END_NODE_NOT_FOUND("A0043", "没有结束节点"),
    A_WF_EDGE_NOT_FOUND("A0044", "工作流的边找不到"),
    A_WF_RUNTIME_NOT_FOUND("A00045", "工作流运行时数据找不到"),
    A_SEARCH_QUERY_IS_EMPTY("A00046", "搜索内容不能为空"),
    A_WF_COMPONENT_NOT_FOUND("A00047", "工作流基础组件找不到"),
    A_WF_RESUME_FAIL("A00048", "工作流恢复执行时失败"),
    A_MAIL_SENDER_EMPTY("A00049", "邮件发送人不能为空"),
    A_MAIL_SENDER_CONFIG_ERROR("A00050", "邮件发送人配置错误"),
    A_MAIL_RECEIVER_EMPTY("A00051", "邮件接收人不能为空"),
    A_MCP_SERVER_NOT_FOUND("A00052", "MCP服务找不到"),
    A_USER_MCP_SERVER_NOT_FOUND("A00053", "用户的MCP服务找不到"),
    A_PARAMS_INVALID_BY_("A00054", "参数校验异常:{0}"),
    A_AI_MESSAGE_NOT_FOUND("A00055", "找不到AI的消息"),
    A_USER_QUESTION_NOT_FOUND("A00056", "用户问题不存在"),
    A_PLATFORM_NOT_MATCH("A0057", "平台不匹配"),
    B_UNCAUGHT_ERROR("B0001", "未捕捉异常"),
    B_COMMON_ERROR("B0002", "业务出错"),
    B_GLOBAL_ERROR("B0003", "全局异常"),
    B_SAVE_IMAGE_ERROR("B0004", "保存图片异常"),
    B_FIND_IMAGE_404("B0005", "无法找到图片"),
    B_DAILY_QUOTA_USED("B0006", "今天额度已经用完"),
    B_MONTHLY_QUOTA_USED("B0007", "当月额度已经用完"),
    B_LLM_NOT_SUPPORT("B0008", "LLM不支持该功能"),
    B_LLM_SECRET_KEY_NOT_SET("B0009", "LLM的secret key没设置"),
    B_MESSAGE_NOT_FOUND("B0008", "消息不存在"),
    B_LLM_SERVICE_DISABLED("B0009", "LLM服务不可用"),
    B_KNOWLEDGE_BASE_IS_EMPTY("B0010", "知识库内容为空"),
    B_NO_ANSWER("B0011", "[无答案]"),
    B_SAVE_FILE_ERROR("B0012", "保存文件异常"),
    B_BREAK_SEARCH("B0013", "中断搜索"),
    B_GRAPH_FILTER_NOT_FOUND("B0014", "图过滤器未定义"),
    B_DB_ERROR("B0015", "数据库查询异常"),
    B_ACTIVE_USER_ERROR("B0016", "激活用户失败"),
    B_RESET_PASSWORD_ERROR("B0017", "重置密码失败"),
    B_IMAGE_LOAD_ERROR("B0018", "加载图片失败"),
    B_IO_EXCEPTION("B0019", "IO异常"),
    B_SERVER_EXCEPTION("B0020", "服务端异常"),
    B_DELETE_FILE_ERROR("B0021", "删除文件异常"),
    B_WF_RUN_ERROR("B0022", "工作流运行异常"),
    B_WF_NODE_DEFINITION_NOT_FOUND("B0023", "工作流节点定义找不到"),
    B_DIR_CREATE_FAIL("B0024", "创建目录失败"),
    B_LLM_TEMPERATURE_ERROR("B0025", "采样温度应该在 0.1-1之间"),
    B_ASR_SETTING_NOT_FOUND("B0026", "语音识别设置未找到"),
    B_URL_INVALID("B0027", "不是有效的网络地址"),
    B_ASR_MODEL_NOT_FOUND("B0028", "语音识别模型未找到"),
    B_TTS_SETTING_NOT_FOUND("B0029", "语音合成设置未找到"),
    B_TTS_MODEL_NOT_FOUND("B0030", "语音合成模型未找到"),
    B_VOICE_NOT_FOUND("B0031", "声音不存在"),
    C_DRAW_FAIL("C0001", "大模型生成图片失败,原因:{0}"),
    C_ALI_OSS_CONFIG_ERROR("C0002", "阿里云OSS初始化失败,原因:{0}"),
    C_LLM_RESPONSE_INVALID("C0003", "大模型生成结果内容无效"),
    C_WF_COMPONENT_DELETED_FAIL_BY_USED("C0004", "工作流组件已经被使用，无法被删除，可先停用");

    private final String code;
    private final String info;

    ErrorEnum(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public static ErrorEnum getErrorEnum(String code) {
        ErrorEnum result = null;
        for (ErrorEnum c : ErrorEnum.values()) {
            if (c.getCode().equals(code)) {
                result = c;
                break;
            }
        }
        if (null == result) {
            result = B_COMMON_ERROR;
        }
        return result;
    }

}
