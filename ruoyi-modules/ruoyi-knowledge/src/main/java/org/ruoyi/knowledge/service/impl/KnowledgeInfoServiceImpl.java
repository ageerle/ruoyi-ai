package org.ruoyi.knowledge.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.model.LoginUser;
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
import org.ruoyi.knowledge.domain.bo.KnowledgeInfoBo;
import org.ruoyi.knowledge.domain.req.KnowledgeInfoUploadRequest;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.mapper.KnowledgeAttachMapper;
import org.ruoyi.knowledge.mapper.KnowledgeFragmentMapper;
import org.ruoyi.knowledge.mapper.KnowledgeInfoMapper;
import org.ruoyi.knowledge.service.EmbeddingService;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeInfo::getKid, bo.getKid());
        lqw.eq(bo.getUid() != null, KnowledgeInfo::getUid, bo.getUid());
        lqw.like(StringUtils.isNotBlank(bo.getKname()), KnowledgeInfo::getKname, bo.getKname());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription, bo.getDescription());
        // 查询公开的知识库
        lqw.or(wrapper -> wrapper.eq(KnowledgeInfo::getShare, "1"));
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
            chunkList = resourceLoader.getChunkList(content, kid);
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
        List<KnowledgeInfoVo> knowledgeInfoList = baseMapper.selectVoByMap(map);
        check(knowledgeInfoList);
        // 删除知识库
        baseMapper.deleteByMap(map);
        // 删除附件和知识片段
        fragmentMapper.deleteByMap(map);
        attachMapper.deleteByMap(map);
        // 删除向量库信息
        embeddingService.removeByKid(id);
    }

    @Override
    public void check(List<KnowledgeInfoVo> knowledgeInfoList){
        LoginUser loginUser = LoginHelper.getLoginUser();
        for (KnowledgeInfoVo knowledgeInfoVo : knowledgeInfoList) {
            if(!knowledgeInfoVo.getUid().equals(loginUser.getUserId())){
                throw new SecurityException("权限不足");
            }
        }
    }
}
