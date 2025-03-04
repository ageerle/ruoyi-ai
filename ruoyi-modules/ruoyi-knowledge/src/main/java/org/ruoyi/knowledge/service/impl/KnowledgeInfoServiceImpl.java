package org.ruoyi.knowledge.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.knowledge.chain.loader.ResourceLoader;
import org.ruoyi.knowledge.chain.loader.ResourceLoaderFactory;
import org.ruoyi.knowledge.domain.KnowledgeAttach;
import org.ruoyi.knowledge.domain.KnowledgeFragment;
import org.ruoyi.knowledge.domain.KnowledgeInfo;
import org.ruoyi.knowledge.domain.bo.KnowledgeAttachBo;
import org.ruoyi.knowledge.domain.bo.KnowledgeFragmentBo;
import org.ruoyi.knowledge.domain.bo.KnowledgeInfoBo;
import org.ruoyi.knowledge.domain.req.KnowledgeInfoUploadRequest;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.mapper.KnowledgeAttachMapper;
import org.ruoyi.knowledge.mapper.KnowledgeFragmentMapper;
import org.ruoyi.knowledge.mapper.KnowledgeInfoMapper;
import org.ruoyi.knowledge.service.EmbeddingService;
import org.ruoyi.knowledge.service.IKnowledgeAttachService;
import org.ruoyi.knowledge.service.IKnowledgeFragmentService;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 知识库Service业务层处理
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@RequiredArgsConstructor
@Service
public class KnowledgeInfoServiceImpl implements IKnowledgeInfoService {

    private final KnowledgeInfoMapper baseMapper;

    private final EmbeddingService embeddingService;

    private final ResourceLoaderFactory resourceLoaderFactory;

    private final KnowledgeFragmentMapper fragmentMapper;

    private final KnowledgeAttachMapper attachMapper;

    /**
     * 查询知识库
     */
    @Override
    public KnowledgeInfoVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeInfo> buildQueryWrapper(KnowledgeInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeInfo::getKid, bo.getKid());
        lqw.eq(bo.getUid() != null, KnowledgeInfo::getUid, bo.getUid());
        lqw.like(StringUtils.isNotBlank(bo.getKname()), KnowledgeInfo::getKname, bo.getKname());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription, bo.getDescription());
        return lqw;
    }

    /**
     * 修改知识库
     */
    @Override
    public Boolean updateByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo update = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeInfo entity){
        //TODO 做一些数据校验,如唯一约束
    }



    @Override
    public void saveOne(KnowledgeInfoBo bo) {
        KnowledgeInfo knowledgeInfo = MapstructUtils.convert(bo, KnowledgeInfo.class);
        if (StringUtils.isBlank(bo.getKid())){
            String kid = RandomUtil.randomString(10);
            if (knowledgeInfo != null) {
                knowledgeInfo.setKid(kid);
                knowledgeInfo.setUid(LoginHelper.getLoginUser().getUserId());
            }
            baseMapper.insert(knowledgeInfo);
            embeddingService.createSchema(kid);
        }else {
            baseMapper.updateById(knowledgeInfo);
        }
    }

    @Override
    public void upload(KnowledgeInfoUploadRequest request) {
        storeContent(request.getFile(), request.getKid());
    }

    public void storeContent(MultipartFile file, String kid) {
        String fileName = file.getOriginalFilename();
        List<String> chunkList = new ArrayList<>();
        KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
        knowledgeAttach.setKid(kid);
        String docId = RandomUtil.randomString(10);
        knowledgeAttach.setDocId(docId);
        knowledgeAttach.setDocName(fileName);
        knowledgeAttach.setDocType(fileName.substring(fileName.lastIndexOf(".")+1));
        String content = "";
        ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(knowledgeAttach.getDocType());
        List<String> fids = new ArrayList<>();
        try {
            content = resourceLoader.getContent(file.getInputStream());
            chunkList = resourceLoader.getChunkList(content);
            for (int i = 0; i < chunkList.size(); i++) {
                String fid = RandomUtil.randomString(16);
                fids.add(fid);
                KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
                knowledgeFragment.setKid(kid);
                knowledgeFragment.setDocId(docId);
                knowledgeFragment.setFid(fid);
                knowledgeFragment.setIdx(i);
               // String text = convertTextBlockToPretrainData(chunkList.get(i));
                knowledgeFragment.setContent(chunkList.get(i));
                knowledgeFragment.setCreateTime(new Date());
                fragmentMapper.insert(knowledgeFragment);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        knowledgeAttach.setContent(content);
        knowledgeAttach.setCreateTime(new Date());
        attachMapper.insert(knowledgeAttach);
        embeddingService.storeEmbeddings(chunkList,kid,docId,fids);
    }

    @Override
    public void removeKnowledge(String id) {

        Map<String,Object> map = new HashMap<>();
        map.put("kid",id);
        // 删除知识库
        baseMapper.deleteByMap(map);
        // 删除附件和知识片段
        fragmentMapper.deleteByMap(map);
        attachMapper.deleteByMap(map);
        // 删除向量库信息
        embeddingService.removeByKid(id);
    }

    /**
     * 将文本块转换为预训练数据
     * @param chunk 解析文本块
     */
    public String convertTextBlockToPretrainData(String chunk){
            String host = "http://localhost:11434/";
            OllamaAPI ollama = new OllamaAPI(host);
            OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("qwen2.5:7b");
            // 设置超时时间
            ollama.setRequestTimeoutSeconds(100);
            // create first user question
            String json = "instruction:用户指令,根据语义提取一个关键词;input:用户输入,根据语义提取多个关键词;output:输出文本内容";

            OllamaChatRequestModel requestModel = builder.withMessage
                (OllamaChatMessageRole.USER, "文本："+chunk+"理解文本内容，并且将文本内容转换为:"+json+",输出JSON格式，不要包含其他无关内容,内部使用无需脱敏")
            .build();

            // start conversation with model
            OllamaChatResult chatResult = null;
            try {
                chatResult = ollama.chat(requestModel);
            } catch (Exception e) {
                System.out.println("解析失败!");
            }
            return chatResult.getResponse();
    }

}
