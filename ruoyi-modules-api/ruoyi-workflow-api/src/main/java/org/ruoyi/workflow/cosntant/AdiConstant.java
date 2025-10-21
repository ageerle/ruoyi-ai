package org.ruoyi.workflow.cosntant;

import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AdiConstant {
    public static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * 验证码id过期时间：1小时
     */
    public static final int AUTH_CAPTCHA_ID_EXPIRE = 1;
    /**
     * 验证码过期时间，5分钟
     */
    public static final int AUTH_CAPTCHA_EXPIRE = 5;
    /**
     * 注册激活码有效时长，8小时
     */
    public static final int AUTH_ACTIVE_CODE_EXPIRE = 8;
    /**
     * token存活时间（8小时）
     */
    public static final int USER_TOKEN_EXPIRE = 8;
    public static final String DEFAULT_PASSWORD = "123456";
    public static final int LOGIN_MAX_FAIL_TIMES = 3;
    public static final String[] WEB_RESOURCES = {
            "/swagger-ui/index.html",
            "/swagger-ui",
            "/swagger-resources",
            "/v3/api-docs",
            "/favicon.ico",
            ".css",
            ".js",
            "/doc.html"
    };
    public static final int SECRET_KEY_TYPE_SYSTEM = 1;
    public static final int SECRET_KEY_TYPE_CUSTOM = 2;
    public static final String OPENAI_MESSAGE_DONE_FLAG = "[DONE]";
    public static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    public static final String CREATE_IMAGE_RESP_FORMATS_B64JSON = "b64_json";
    public static final String OPENAI_CREATE_IMAGE_RESP_FORMATS_URL = "url";
    public static final List<String> DALLE2_CREATE_IMAGE_SIZES = List.of("256x256", "512x512", "1024x1024");
    public static final List<String> DALLE3_CREATE_IMAGE_SIZES = List.of("1024x1024", "1024x1792", "1792x1024");
    public static final PromptTemplate PROMPT_EXTRA_TEMPLATE = PromptTemplate.from("""
            ## 要求
            尽可能准确地回答用户的问题
                        
            ## 用户的问题
            {{question}}
                        
            ## 注意
            {{extraInfo}}
            """);
    public static final PromptTemplate PROMPT_INFO_TEMPLATE = PromptTemplate.from("""
            ## 要求
            根据已知信息，尽可能准确地回答用户的问题
                        
            ## 用户的问题
            {{question}}
                        
            ## 已知信息
            {{information}}
                        
            ## 注意
            回答的内容不能让用户感知到已知信息的存在
            """);
    /**
     * 可能的 extraInfo 如适用转音频的要求： 2. 回答的内容要尽量口语化，以方便将内容转成语音
     */
    public static final PromptTemplate PROMPT_INFO_EXTRA_TEMPLATE = PromptTemplate.from("""
            ## 要求
            根据已知信息，尽可能准确地回答用户的问题
                        
            ## 用户的问题
            {{question}}
                        
            ## 已知信息
            {{information}}
                        
            ## 注意
            1. 回答的内容不能让用户感知到已知信息的存在
            {{extraInfo}}
            """);
    public static final String PROMPT_EXTRA_AUDIO = "2. 回答的内容要尽量口语化，以方便将内容转成语音";
    public static final Double LLM_TEMPERATURE_DEFAULT = 0.7D;
    public static final Double RAG_RETRIEVE_MIN_SCORE_DEFAULT = 0.6D;
    public static final int tts_ = 1;
    public static final String[] POI_DOC_TYPES = {"doc", "docx", "ppt", "pptx", "xls", "xlsx"};
    public static final long SSE_TIMEOUT = (2 * 60 + 30) * 1000L; // 2.5分钟
    public static final int RAG_TYPE_KB = 1;
    public static final int RAG_TYPE_SEARCH = 2;
    /**
     * 每块文档长度（按token算）
     */
    public static final int RAG_MAX_SEGMENT_SIZE_IN_TOKENS = 1000;
    /**
     * 文档召回默认数量
     */
    public static final int RAG_RETRIEVE_NUMBER_DEFAULT = 3;
    /**
     * 文档召回最大数量
     */
    public static final int RAG_RETRIEVE_NUMBER_MAX = 5;
    /**
     * 向量搜索时命中所需的最低分数
     */
    public static final double RAG_MIN_SCORE = 0.6;
    /**
     * 默认的最大输入token数
     */
    public static final int LLM_MAX_INPUT_TOKENS_DEFAULT = 4096;
    public static final String LLM_INPUT_TYPE_TEXT = "text";
    public static final String LLM_INPUT_TYPE_IMAGE = "image";
    public static final String LLM_INPUT_TYPE_AUDIO = "audio";
    public static final String LLM_INPUT_TYPE_VIDEO = "video";
    public static final String[] GRAPH_ENTITY_EXTRACTION_ENTITY_TYPES = {"organization", "person", "geo", "event"};
    public static final String GRAPH_TUPLE_DELIMITER = "<|>";
    public static final String GRAPH_RECORD_DELIMITER = "##";
    public static final String GRAPH_COMPLETION_DELIMITER = "<|COMPLETE|>";
    public static final List<String> GRAPH_STORE_MAIN_FIELDS = List.of("name", "label", "text_segment_id", "description");
    /**
     * 唯一标识字段，如果该字段有指定，则根据该配置判断Vertex或Edge是否唯一，如知识库中根据 name、metadata->>kb_uuid 来做判断
     */
    public static final String GRAPH_METADATA_IDENTIFY_COLUMNS = "graph_metadata_identify_columns";
    /**
     * 内容追加字段
     * 更新数据时，如遇到该标识中的字段，追加内容而不是替换
     */
    public static final String GRAPH_METADATA_APPEND_COLUMNS = "graph_metadata_append_columns_if_exist";
    public static final int AI_IMAGE_TYPE_REGULAR = 1;
    public static final int AI_IMAGE_TYPE_THUMBNAIL = 2;
    public static final int AI_IMAGE_TYPE_REGULAR_MARK = 3;
    public static final int AI_IMAGE_TYPE_THUMBNAIL_MARK = 4;
    public static final String DOC_INDEX_TYPE_EMBEDDING = "embedding";
    public static final String DOC_INDEX_TYPE_GRAPHICAL = "graphical";
    public static final String DRAW_TYPE_PUBLIC = "public";
    public static final String DRAW_TYPE_STARRED = "starred";
    public static final String DRAW_TYPE_MINE = "mine";
    public static final String MP_LIMIT_1 = "limit 1";
    /**
     * 文件存储在本地
     */
    public static final int STORAGE_LOCATION_LOCAL = 1;
    /**
     * 文件存储到阿里云OSS
     */
    public static final int STORAGE_LOCATION_ALI_OSS = 2;
    public static final String URL_PREFIX_FILE = "/file/";
    public static final String URL_PREFIX_IMAGE = "/image/";
    public static final String URL_PREFIX_MY_IMAGE = "/my-image/";
    public static final String URL_PREFIX_MY_THUMBNAIL = "/my-thumbnail/";
    public static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    public static final String W_FAILED = "FAILED";
    public static final String COLUMN_NAME_IS_DELETE = "is_deleted";
    public static final String COLUMN_NAME_USER_ID = "user_id";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_UUID = "uuid";
    public static final String FORM_DATA_BOUNDARY_PRE = "----WebKitFormBoundary";

    private AdiConstant() {
    }

    public static class ConversationConstant {
        public static final String DEFAULT_NAME = "通用智能助手";
        public static final int ANSWER_CONTENT_TYPE_AUTO = 1;
        public static final int ANSWER_CONTENT_TYPE_TEXT = 2;
        public static final int ANSWER_CONTENT_TYPE_AUDIO = 3;
        public static final String AUDIO_CONFIG_FIELD_ANSWER_VOICE = "answer_voice";
        public static final String AUDIO_CONFIG_FIELD_VOICE_PLATFORM = "platform";

        private ConversationConstant() {
        }
    }

    public static class GenerateImage {
        public static final int INTERACTING_METHOD_GENERATE_IMAGE = 1;
        public static final int INTERACTING_METHOD_EDIT_IMAGE = 2;
        public static final int INTERACTING_METHOD_VARIATION = 3;
        public static final int INTERACTING_METHOD_BACKGROUND_GENERATION = 4;
        public static final int STATUS_DOING = 1;
        public static final int STATUS_FAIL = 2;
        public static final int STATUS_SUCCESS = 3;

        private GenerateImage() {
        }
    }

    public static class MetadataKey {
        public static final String KB_UUID = "kb_uuid";
        public static final String KB_ITEM_UUID = "kb_item_uuid";
        public static final String ENGINE_NAME = "engine_name";
        public static final String SEARCH_UUID = "search_uuid";

        private MetadataKey() {
        }
    }

    public static class SysConfigKey {
        public static final String DEEPSEEK_SETTING = "deepseek_setting";
        public static final String OPENAI_SETTING = "openai_setting";
        public static final String DASHSCOPE_SETTING = "dashscope_setting";
        public static final String QIANFAN_SETTING = "qianfan_setting";
        public static final String OLLAMA_SETTING = "ollama_setting";
        public static final String SILICONFLOW_SETTING = "siliconflow_setting";
        public static final String GOOGLE_SETTING = "google_setting";
        public static final String BING_SETTING = "bing_setting";
        public static final String BAIDU_SETTING = "baidu_setting";
        public static final String REQUEST_TEXT_RATE_LIMIT = "request_text_rate_limit";
        public static final String REQUEST_IMAGE_RATE_LIMIT = "request_image_rate_limit";
        public static final String CONVERSATION_MAX_NUM = "conversation_max_num";
        public static final String QUOTA_BY_TOKEN_DAILY = "quota_by_token_daily";
        public static final String QUOTA_BY_TOKEN_MONTHLY = "quota_by_token_monthly";
        public static final String QUOTA_BY_REQUEST_DAILY = "quota_by_request_daily";
        public static final String QUOTA_BY_REQUEST_MONTHLY = "quota_by_request_monthly";
        public static final String QUOTA_BY_IMAGE_DAILY = "quota_by_image_daily";
        public static final String QUOTA_BY_IMAGE_MONTHLY = "quota_by_image_monthly";
        public static final String QUOTA_BY_QA_ASK_DAILY = "quota_by_qa_ask_daily";
        public static final String STORAGE_LOCATION = "storage_location";
        public static final String STORAGE_LOCATION_ALI_OSS = "storage_location_ali_oss";
        public static final String ASR_SETTING = "asr_setting";
        public static final String TTS_SETTING = "tts_setting";

        private SysConfigKey() {
        }
    }

    public static class ModelPlatform {
        public static final String DEEPSEEK = "deepseek";
        public static final String OPENAI = "openai";
        public static final String DASHSCOPE = "dashscope";
        public static final String QIANFAN = "qianfan";
        public static final String OLLAMA = "ollama";
        public static final String SILICONFLOW = "siliconflow";

        private ModelPlatform() {
        }

        // 获取所有公共静态常量（String类型的值）的列表
        public static List<String> getModelConstants() {
            List<String> list = new ArrayList<>();
            Class<ModelPlatform> clazz = ModelPlatform.class;
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String value = (String) field.get(null);
                    list.add(value);
                } catch (ReflectiveOperationException e) {
                    log.error("error", e);
                }

            }
            return list;
        }
    }

    public static class ModelType {
        public static final String TEXT = "text";
        public static final String IMAGE = "image";
        public static final String EMBEDDING = "embedding";
        public static final String RERANK = "rerank";
        public static final String ASR = "asr";
        public static final String TTS = "tts";

        private ModelType() {
        }

        public static List<String> getModelType() {
            List<String> list = new ArrayList<>();
            Class<ModelType> clazz = ModelType.class;
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String value = (String) field.get(null);
                    list.add(value);
                } catch (ReflectiveOperationException e) {
                    log.error("error", e);
                }

            }
            return list;
        }
    }

    public static class SearchEngineName {
        public static final String GOOGLE = "google";
        public static final String BING = "bing";
        public static final String BAIDU = "baidu";
        public static final String[] GOOGLE_COUNTRIES = {"cn", "af", "al", "dz", "as", "ad", "ao", "ai", "aq", "ag", "ar", "am", "aw", "au", "at", "az", "bs", "bh", "bd", "bb", "by", "be", "bz", "bj", "bm", "bt", "bo", "ba", "bw", "bv", "br", "io", "bn", "bg", "bf", "bi", "kh", "cm", "ca", "cv", "ky", "cf", "td", "cl", "cx", "cc", "co", "km", "cg", "cd", "ck", "cr", "ci", "hr", "cu", "cy", "cz", "dk", "dj", "dm", "do", "ec", "eg", "sv", "gq", "er", "ee", "et", "fk", "fo", "fj", "fi", "fr", "gf", "pf", "tf", "ga", "gm", "ge", "de", "gh", "gi", "gr", "gl", "gd", "gp", "gu", "gt", "gn", "gw", "gy", "ht", "hm", "va", "hn", "hk", "hu", "is", "in", "id", "ir", "iq", "ie", "il", "it", "jm", "jp", "jo", "kz", "ke", "ki", "kp", "kr", "kw", "kg", "la", "lv", "lb", "ls", "lr", "ly", "li", "lt", "lu", "mo", "mk", "mg", "mw", "my", "mv", "ml", "mt", "mh", "mq", "mr", "mu", "yt", "mx", "fm", "md", "mc", "mn", "ms", "ma", "mz", "mm", "na", "nr", "np", "nl", "an", "nc", "nz", "ni", "ne", "ng", "nu", "nf", "mp", "no", "om", "pk", "pw", "ps", "pa", "pg", "py", "pe", "ph", "pn", "pl", "pt", "pr", "qa", "re", "ro", "ru", "rw", "sh", "kn", "lc", "pm", "vc", "ws", "sm", "st", "sa", "sn", "rs", "sc", "sl", "sg", "sk", "si", "sb", "so", "za", "gs", "es", "lk", "sd", "sr", "sj", "sz", "se", "ch", "sy", "tw", "tj", "tz", "th", "tl", "tg", "tk", "to", "tt", "tn", "tr", "tm", "tc", "tv", "ug", "ua", "ae", "uk", "gb", "us", "um", "uy", "uz", "vu", "ve", "vn", "vg", "vi", "wf", "eh", "ye", "zm", "zw"};
        public static final String[] GOOGLE_LANGUAGES = {"zh-cn", "zh-tw", "af", "ak", "sq", "ws", "am", "ar", "hy", "az", "eu", "be", "bem", "bn", "bh", "xx-bork", "bs", "br", "bg", "bt", "km", "ca", "chr", "ny", "co", "hr", "cs", "da", "nl", "xx-elmer", "en", "eo", "et", "ee", "fo", "tl", "fi", "fr", "fy", "gaa", "gl", "ka", "de", "el", "kl", "gn", "gu", "xx-hacker", "ht", "ha", "haw", "iw", "hi", "hu", "is", "ig", "id", "ia", "ga", "it", "ja", "jw", "kn", "kk", "rw", "rn", "xx-klingon", "kg", "ko", "kri", "ku", "ckb", "ky", "lo", "la", "lv", "ln", "lt", "loz", "lg", "ach", "mk", "mg", "ms", "ml", "mt", "mv", "mi", "mr", "mfe", "mo", "mn", "sr-me", "my", "ne", "pcm", "nso", "no", "nn", "oc", "or", "om", "ps", "fa", "xx-pirate", "pl", "pt", "pt-br", "pt-pt", "pa", "qu", "ro", "rm", "nyn", "ru", "gd", "sr", "sh", "st", "tn", "crs", "sn", "sd", "si", "sk", "sl", "so", "es", "es-419", "su", "sw", "sv", "tg", "ta", "tt", "te", "th", "ti", "to", "lua", "tum", "tr", "tk", "tw", "ug", "uk", "ur", "uz", "vu", "vi", "cy", "wo", "xh", "yi", "yo", "zu"};

        private SearchEngineName() {
        }
    }

    public static class SSEEventName {
        public static final String START = "[START]";
        public static final String DONE = "[DONE]";
        public static final String ERROR = "[ERROR]";
        public static final String META = "[META]";
        public static final String AUDIO = "[AUDIO]";
        public static final String THINKING = "[THINKING]";
        public static final String AI_SEARCH_SOURCE_LINKS = "[SOURCE_LINKS]";
        public static final String WF_NODE_CHUNK = "[WF_NODE_CHUNK]";
        public static final String WF_NODE_OUTPUT = "[WF_NODE_OUTPUT]";
        public static final String STATE_CHANGED = "[STATE_CHANGED]";

        private SSEEventName() {
        }
    }

    public static class SSEEventData {

        /**
         * 状态：问题分析中
         * 如敏感词校验等
         */
        public static final String STATE_QUESTION_ANALYSING = """
                {"state":"question_analysing","remark":"问题分析中"}
                """;

        public static final String STATE_KNOWLEDGE_SEARCHING = """
                {"state":"knowledge_searching","remark":"知识库搜索中"}
                """;
        //使用 THINKING 事件代替
        public static final String STATE_THINKING = """
                {"state":"thinking","remark":"推理中"}
                """;
        public static final String STATE_RESPONDING = """
                {"state":"responding","remark":"回答中"}
                """;
    }

    public static class WorkflowConstant {
        public static final String DEFAULT_INPUT_PARAM_NAME = "input";
        public static final String DEFAULT_OUTPUT_PARAM_NAME = "output";
        public static final String DEFAULT_ERROR_OUTPUT_PARAM_NAME = "error_msg";
        public static final String HUMAN_FEEDBACK_KEY = "human_feedback";
        public static final int NODE_PROCESS_STATUS_READY = 1;
        public static final int NODE_PROCESS_STATUS_DOING = 2;
        public static final int NODE_PROCESS_STATUS_SUCCESS = 3;
        public static final int NODE_PROCESS_STATUS_FAIL = 4;

        public static final int WORKFLOW_PROCESS_STATUS_READY = 1;
        public static final int WORKFLOW_PROCESS_STATUS_DOING = 2;
        public static final int WORKFLOW_PROCESS_STATUS_SUCCESS = 3;
        public static final int WORKFLOW_PROCESS_STATUS_FAIL = 4;
        public static final int WORKFLOW_PROCESS_STATUS_WAITING_INPUT = 5;

        public static final int WORKFLOW_NODE_PROCESS_TYPE_NORMAL = 1;
        public static final int WORKFLOW_NODE_PROCESS_TYPE_CONDITIONAL = 2;
        public static final int WORKFLOW_NODE_PROCESS_TYPE_PARALLEL = 3;

        public static final int MAIL_SENDER_TYPE_SYS = 1;
        public static final int MAIL_SENDER_TYPE_CUSTOM = 2;
    }

    public static class TokenEstimator {
        public static String OPENAI = "openai";
        public static String HUGGING_FACE = "huggingface";
        public static String QWEN = "qwen";

        public static List<String> ALL = List.of(OPENAI, HUGGING_FACE, QWEN);
    }

    public static class EmbeddingModel {
        public static String ALL_MINILM_L6 = "local:all-minilm-l6-v2";
    }

    public static class McpConstant {
        public static final String TRANSPORT_TYPE_SSE = "sse";
        public static final String TRANSPORT_TYPE_STDIO = "stdio";
        public static final String INSTALL_TYPE_REMOTE = "remote";
        public static final String INSTALL_TYPE_WASM = "wasm";
        public static final String INSTALL_TYPE_LOCAL = "local";
        public static final String INSTALL_TYPE_DOCKER = "docker";
    }

    public static class TtsConstant {

        /**
         * 语音合成器位置-客户端
         */
        public static final String SYNTHESIZER_CLIENT = "client";
        /**
         * 语音合成器位置-服务端
         */
        public static final String SYNTHESIZER_SERVER = "server";

        /**
         * 通义默认语音音色-龙应严(义正严辞女声)
         */
        public static final String DASHSCOPE_DEFAULT_VOICE = "longyingyan";
    }

    public static class CustomChatRequestParameterKeys {
        /**
         * 是否开启思考模式，默认不开启
         */
        public static final String ENABLE_THINKING = "enable_thinking";

        private CustomChatRequestParameterKeys() {
        }
    }
}
