package org.ruoyi.chat.service.chat.impl;

import cn.hutool.core.collection.CollectionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.ruoyi.chat.factory.ChatServiceFactory;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.service.chat.ISseService;
import org.ruoyi.chat.util.SSEUtil;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.core.utils.DateUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.core.utils.file.MimeTypeUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.bo.ChatSessionBo;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.IChatSessionService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.VectorStoreService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ageer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements ISseService {

    private final OpenAiStreamClient openAiStreamClient;

    private final VectorStoreService vectorStoreService;

    private final IChatCostService chatCostService;

    private final IChatModelService chatModelService;

    private final ChatServiceFactory chatServiceFactory;

    private final IChatSessionService chatSessionService;

    private final IKnowledgeInfoService knowledgeInfoService;

    private ChatModelVo chatModelVo;


    @Override
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        try {
            // 构建消息列表
            buildChatMessageList(chatRequest);
            // 设置对话角色
            chatRequest.setRole(Message.Role.USER.getName());

            if(LoginHelper.isLogin()){
                // 保存消息记录 并扣除费用
                chatCostService.deductToken(chatRequest);
                chatRequest.setUserId(chatCostService.getUserId());
                if(chatRequest.getSessionId()==null){
                    ChatSessionBo chatSessionBo = new ChatSessionBo();
                    chatSessionBo.setUserId(chatCostService.getUserId());
                    chatSessionBo.setSessionTitle(getFirst10Characters(chatRequest.getPrompt()));
                    chatSessionBo.setSessionContent(chatRequest.getPrompt());
                    chatSessionService.insertByBo(chatSessionBo);
                    chatRequest.setSessionId(chatSessionBo.getId());
                }
            }
            // 根据模型分类调用不同的处理逻辑
            IChatService chatService = chatServiceFactory.getChatService(chatModelVo.getCategory());
            chatService.chat(chatRequest, sseEmitter);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            SSEUtil.sendErrorEvent(sseEmitter,e.getMessage());
        }
        return sseEmitter;
    }

    /**
     * 获取对话标题
     *
     * @param str 原字符
     * @return 截取后的字符
     */
    public static String getFirst10Characters(String str) {
        // 判断字符串长度
        if (str.length() > 10) {
            // 如果长度大于10，截取前10个字符
            return str.substring(0, 10);
        } else {
            // 如果长度不足10，返回整个字符串
            return str;
        }
    }

    /**
     *  构建消息列表
     */
    private void buildChatMessageList(ChatRequest chatRequest){
        String sysPrompt;
        // 矫正模型名称 如果是gpt-image 则查询image类型模型 获取模型名称
        if(chatRequest.getModel().equals("gpt-image")) {
            chatModelVo = chatModelService.selectModelByCategory("image");
            if (chatModelVo == null) {
                log.error("未找到image类型的模型配置");
                throw new IllegalStateException("未找到image类型的模型配置");
            }//            chatRequest.setModel(chatModelVo.getModelName());
        }else{
            chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        }
        // 获取对话消息列表
        List<Message> messages = chatRequest.getMessages();
        // 查询向量库相关信息加入到上下文
        if(StringUtils.isNotEmpty(chatRequest.getKid())){
            List<Message> knMessages = new ArrayList<>();
            String content = messages.get(messages.size() - 1).getContent().toString();
            // 通过kid查询知识库信息
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKid()));
            // 查询向量模型配置信息
            ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModelName());

            QueryVectorBo queryVectorBo = new QueryVectorBo();
            queryVectorBo.setQuery(content);
            queryVectorBo.setKid(chatRequest.getKid());
            queryVectorBo.setApiKey(chatModel.getApiKey());
            queryVectorBo.setBaseUrl(chatModel.getApiHost());
            queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModelName());
            queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModelName());
            queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());
            List<String> nearestList = vectorStoreService.getQueryVector(queryVectorBo);
            for (String prompt : nearestList) {
                Message userMessage = Message.builder().content(prompt).role(Message.Role.USER).build();
                knMessages.add(userMessage);
            }
            messages.addAll(knMessages);
            // 设置知识库系统提示词
            sysPrompt = knowledgeInfoVo.getSystemPrompt();
            if(StringUtils.isEmpty(sysPrompt)){
                sysPrompt ="###角色设定\n" +
                        "你是一个智能知识助手，专注于利用上下文中的信息来提供准确和相关的回答。\n" +
                        "###指令\n" +
                        "当用户的问题与上下文知识匹配时，利用上下文信息进行回答。如果问题与上下文不匹配，运用自身的推理能力生成合适的回答。\n" +
                        "###限制\n" +
                        "确保回答清晰简洁，避免提供不必要的细节。始终保持语气友好" +
                        "当前时间："+ DateUtils.getDate();
            }
        }else {
            sysPrompt = chatModelVo.getSystemPrompt();
            if(StringUtils.isEmpty(sysPrompt)){
                sysPrompt ="你是一个由RuoYI-AI开发的人工智能助手，名字叫熊猫助手。你擅长中英文对话，能够理解并处理各种问题，提供安全、有帮助、准确的回答。" +
                        "当前时间："+ DateUtils.getDate()+
                        "#注意：回复之前注意结合上下文和工具返回内容进行回复。";
            }
        }
        // 设置系统默认提示词
        Message sysMessage = Message.builder().content(sysPrompt).role(Message.Role.SYSTEM).build();
        messages.add(0,sysMessage);

        chatRequest.setSysPrompt(sysPrompt);
        // 用户对话内容
        String chatString = null;
        // 获取用户对话信息
        Object content = messages.get(messages.size() - 1).getContent();
        if (content instanceof List<?> listContent) {
            if (CollectionUtil.isNotEmpty(listContent)) {
                chatString = listContent.get(0).toString();
            }
        } else if (content instanceof String) {
            chatString = (String) content;
        }
        // 设置对话信息
        chatRequest.setPrompt(chatString);
    }


    /**
     * 文字转语音
     */
    @Override
    public ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech) {
        ResponseBody body = openAiStreamClient.textToSpeech(textToSpeech);
        if (body != null) {
            // 将ResponseBody转换为InputStreamResource
            InputStreamResource resource = new InputStreamResource(body.byteStream());
            // 创建并返回ResponseEntity
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(resource);
        } else {
            // 如果ResponseBody为空，返回404状态码
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 语音转文字
     */
    @Override
    public WhisperResponse speechToTextTranscriptionsV2(MultipartFile file) {
        // 确保文件不为空
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot convert an empty MultipartFile");
        }
        if (!FileUtils.isValidFileExtention(file, MimeTypeUtils.AUDIO__EXTENSION)) {
            throw new IllegalStateException("File Extention not supported");
        }
        // 创建一个文件对象
        File fileA = new File(System.getProperty("java.io.tmpdir") + File.separator + file.getOriginalFilename());
        try {
            // 将 MultipartFile 的内容写入文件
            file.transferTo(fileA);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert MultipartFile to File", e);
        }
        return openAiStreamClient.speechToTextTranscriptions(fileA);
    }


    @Override
    public UploadFileResponse upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload an empty MultipartFile");
        }
        if (!FileUtils.isValidFileExtention(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION)) {
            throw new IllegalStateException("File Extention not supported");
        }
        return openAiStreamClient.uploadFile("fine-tune", convertMultiPartToFile(file));
    }

    private File convertMultiPartToFile(MultipartFile multipartFile) {
        File file = null;
        try {
            // 获取原始文件名
            String originalFileName = multipartFile.getOriginalFilename();
            // 默认扩展名
            String extension = ".tmp";
            // 尝试从原始文件名中获取扩展名
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // 使用原始文件的扩展名创建临时文件
            Path tempFile = Files.createTempFile(null, extension);
            file = tempFile.toFile();

            // 将MultipartFile的内容写入文件
            try (InputStream inputStream = multipartFile.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } catch (IOException e) {
                // 处理文件写入异常
                e.printStackTrace();
            }
        } catch (IOException e) {
            // 处理临时文件创建异常
            e.printStackTrace();
        }
        return file;
    }

}
